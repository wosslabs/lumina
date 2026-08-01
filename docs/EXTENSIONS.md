# Extending Lumina

Lumina discovers extension descriptors with Java `ServiceLoader`.

## Component descriptors

Implement `io.lumina.plugin.ComponentContribution` and register its class in
`META-INF/services/io.lumina.plugin.ComponentContribution`. Return the wire type and a concise
property-schema description. Browser rendering remains framework-owned in this MVP.

## Themes

Implement `ThemeSpi` and register it through `META-INF/services/io.lumina.plugin.ThemeSpi`. The
SPI returns a CSS resource path. Keep theme CSS limited to Lumina tokens and components.

## AI and transport

Implement `AiProvider` to supply `TokenStream` instances. The WebSocket transport is the supported
transport today; alternate SSE adapters should preserve the same intent and snapshot semantics.
