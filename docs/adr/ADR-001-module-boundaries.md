# ADR-001: Module & package boundaries

## Status
Accepted

## Context
Lumina must remain maintainable for a decade with optional Spring Boot and a Spring-free core.

## Decision
Use multi-module Maven with hard dependency rules from the Phase 1 design spec.
Public API packages: `io.lumina`, `io.lumina.ui`, `io.lumina.state`, `io.lumina.ai`, `io.lumina.spi`.
`lumina-core` and `lumina-runtime` must not depend on Spring, Jetty, or Servlet APIs.
Jetty is confined to `lumina-web`.

## Consequences
Slightly more scaffolding; clear binary-compat and SPI surfaces.
