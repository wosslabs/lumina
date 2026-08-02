# Releasing Lumina

## Prerequisites

1. Apache License 2.0 is in place (`LICENSE`, `NOTICE`).
2. A [Central Portal](https://central.sonatype.com/) account with publishing rights for `io.lumina`
   (or change `groupId` to `io.github.<user>` if you cannot claim `io.lumina`).
3. GPG key for signing (`gpg --list-secret-keys`).
4. Maven `~/.m2/settings.xml` server credentials:

```xml
<servers>
  <server>
    <id>central</id>
    <username>YOUR_CENTRAL_USERNAME</username>
    <password>YOUR_CENTRAL_TOKEN</password>
  </server>
</servers>
```

## Cut a release

```bash
# 1. Ensure main is clean and green
mvn -q clean verify

# 2. Version must be non-SNAPSHOT (already 1.0.0 for this release)
grep '<version>' pom.xml | head -1

# 3. Local install of release bits
mvn -q clean install -DskipTests

# 4. Publish with sources, javadoc, and GPG (does not auto-release)
mvn -q -Prelease clean deploy

# 5. In Central Portal UI, review the deployment and publish

# 6. Tag and push
git tag -a v1.0.0 -m "Lumina 1.0.0"
git push origin v1.0.0
git push origin main
```

## After Central publish

Consumers depend on:

```xml
<dependency>
  <groupId>io.lumina</groupId>
  <artifactId>lumina-web</artifactId>
  <version>1.0.0</version>
</dependency>
```

## GitHub-only soft launch (no Central yet)

If Central namespace approval is pending:

```bash
mvn -q clean verify
git tag -a v1.0.0 -m "Lumina 1.0.0"
git push origin v1.0.0
```

Users build from source until Central is live.
