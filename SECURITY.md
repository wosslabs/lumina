# Security Policy

## Supported versions

Security fixes are accepted for the latest released `1.x` line on `main`.

## Reporting a vulnerability

Please **do not** open a public GitHub issue for security problems.

Email the maintainers via the contact listed on the GitHub repository profile, or open a
**private** security advisory on GitHub (Security → Advisories → New draft advisory).

Include:
- Affected version / commit
- Reproduction steps or PoC
- Impact assessment (RCE, session leak, XSS via framework client, etc.)

We aim to acknowledge reports within 7 days.

## Security notes for deployers

- The embedded server defaults to **loopback** (`127.0.0.1`). Bind to `0.0.0.0` only behind a
  reverse proxy with TLS and access control.
- WebSocket connections enforce an **Origin** allowlist (same-host / localhost by default).
- Do not log intent payloads that may contain PII; `AuditLogger` logs intent names only by design.
- Treat uploaded and download payloads as untrusted; enforce size limits (1 MiB default).
