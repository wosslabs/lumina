# Releasing Lumina

## Prerequisites

1. Apache License 2.0 is in place (`LICENSE`, `NOTICE`).
2. A [Central Portal](https://central.sonatype.com/) account with a **verified** namespace.
   Lumina publishes under `io.github.wosslabs` (not `io.lumina` — that domain is unverified).
   Register `io.github.wosslabs` under [Namespaces](https://central.sonatype.com/publishing/namespaces)
   and complete GitHub org verification (for orgs, Central may ask you to create a temporary
   public verification repo under [wosslabs](https://github.com/wosslabs)).
   Personal `io.github.<user>` auto-provisioning only applies if you signed up with that user.
3. A GPG signing key (see below).
4. Maven `~/.m2/settings.xml` with Central credentials (see below).

## One-time: create a GPG signing key

You currently need a secret key. Create one (replace name/email):

```bash
# Ensure pinentry can prompt in this terminal
export GPG_TTY=$(tty)

cat > /tmp/lumina-gpg-batch <<'EOF'
%echo Generating Lumina release signing key
Key-Type: RSA
Key-Length: 4096
Name-Real: YOUR NAME
Name-Email: YOUR@EMAIL
Expire-Date: 0
%ask-passphrase
%commit
%echo done
EOF

gpg --batch --generate-key /tmp/lumina-gpg-batch
rm /tmp/lumina-gpg-batch

# Confirm the key exists and note the KEY_ID (16 hex chars after rsa4096/)
gpg --list-secret-keys --keyid-format LONG

# Publish the *public* key so Central can verify signatures
KEY_ID=$(gpg --list-secret-keys --keyid-format LONG --with-colons \
  | awk -F: '/^sec:/{print $5; exit}')
gpg --armor --export "$KEY_ID" > /tmp/lumina-public.asc

# Prefer HTTPS upload — gpg --keyserver often fails on macOS ("Host is down" / "No route")
curl -sS -X POST 'https://keyserver.ubuntu.com/pks/add' \
  --data-urlencode "keytext@/tmp/lumina-public.asc"
python3 - <<'PY'
import json, urllib.request
key = open('/tmp/lumina-public.asc').read()
req = urllib.request.Request(
    'https://keys.openpgp.org/vks/v1/upload',
    data=json.dumps({'keytext': key}).encode(),
    headers={'Content-Type': 'application/json'},
    method='POST',
)
print(urllib.request.urlopen(req, timeout=30).read().decode())
PY
# On keys.openpgp.org, verify the email from the returned token email if prompted.
```

If the key was created with placeholder `YOUR NAME <YOUR@EMAIL>`, fix it in your own terminal
(needs passphrase prompt):

```bash
export GPG_TTY=$(tty)
gpg --quick-add-uid 3E47A5FD0FA975CE "Your Real Name <you@example.com>"
gpg --edit-key 3E47A5FD0FA975CE
# type: uid 1   (select the placeholder)
# type: deluid
# type: save
# then re-export and re-upload as above
```

If Maven still cannot find the key, pin it explicitly:

```bash
# ~/.m2/settings.xml (profiles section) OR one-shot:
mvn -q -Prelease clean deploy -Dgpg.keyname="$KEY_ID"
```

On macOS, if signing hangs or fails with pinentry errors:

```bash
export GPG_TTY=$(tty)
# and ensure gpg-agent is running
gpgconf --launch gpg-agent
```

## One-time: Maven Central credentials

Create `~/.m2/settings.xml` (Central Portal token from
https://central.sonatype.com/ → account → Generate User Token):

```xml
<settings>
  <servers>
    <server>
      <id>central</id>
      <username>YOUR_CENTRAL_USERNAME</username>
      <password>YOUR_CENTRAL_TOKEN</password>
    </server>
  </servers>
</settings>
```

The `<id>` must be `central` to match the `central-publishing-maven-plugin`
`publishingServerId` in the parent POM.

## Cut a release

```bash
export GPG_TTY=$(tty)

# 1. Ensure main is clean and green
mvn -q clean verify

# 2. Version must be non-SNAPSHOT
grep '<version>' pom.xml | head -1

# 3. Publish with sources, javadoc, and GPG (does not auto-release)
mvn -q -Prelease clean deploy

# 4. In Central Portal UI, review the deployment and publish
```

Tag `v1.0.0` is already on GitHub for this release; only re-tag if you cut a
new version.

## After Central publish

Consumers depend on:

```xml
<dependency>
  <groupId>io.github.wosslabs</groupId>
  <artifactId>lumina-web</artifactId>
  <version>1.0.0</version>
</dependency>
```

Java packages remain `io.lumina.*`; only the Maven `groupId` is `io.github.wosslabs`.
`lumina-examples` sets `maven.deploy.skip=true` and is not published to Central.

## GitHub-only soft launch (no Central yet)

If Central namespace or GPG setup is still pending, users can build from source
from tag `v1.0.0` until Central is live.
