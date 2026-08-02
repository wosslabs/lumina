# Contributing to Lumina

Thanks for helping improve Lumina. By contributing, you agree that your contributions
are licensed under the [Apache License 2.0](LICENSE).

Please also follow the [Code of Conduct](CODE_OF_CONDUCT.md).

## Development setup

- **Java 25+** and **Maven 3.9+**
- Clone and verify:

```bash
git clone https://github.com/twangdi07/lumina.git
cd lumina
mvn -q clean verify
```

## Run the showcase

```bash
mvn -q -pl lumina-examples -am install
mvn -q -pl lumina-examples exec:java -Dexec.mainClass=io.lumina.examples.showcase.ShowcaseMain
```

Open http://127.0.0.1:8080/

## Pull requests

1. Create a focused branch from `main`.
2. Add or update tests for behavior changes.
3. Run `mvn -q clean verify`.
4. Fill out the PR template.
5. UI changes must satisfy
   [`docs/superpowers/specs/2026-08-01-lumina-ux-constitution-checklist.md`](docs/superpowers/specs/2026-08-01-lumina-ux-constitution-checklist.md).

## Invariants

- **Zero author-written HTML/CSS/JS** for application developers.
- Prefer small, reviewable PRs over large sweeps.
- Do not commit secrets, API keys, or local IDE config.

## Reporting security issues

See [SECURITY.md](SECURITY.md). Do not file public issues for vulnerabilities.

## Questions

Open a GitHub Discussion or Issue for design questions after checking
[docs/GUIDE.md](docs/GUIDE.md) and [docs/VISION.md](docs/VISION.md).
