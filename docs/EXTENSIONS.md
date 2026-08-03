# Extending Lumina

Lumina discovers extension descriptors with Java `ServiceLoader`.

## Component descriptors

Implement `io.lumina.plugin.ComponentContribution` and register its class in
`META-INF/services/io.lumina.plugin.ComponentContribution`. Return the wire type and a concise
property-schema description. Browser rendering remains framework-owned in this MVP.

## Themes

Implement `ThemeSpi` and register it through `META-INF/services/io.lumina.plugin.ThemeSpi`. The
SPI returns a CSS resource path that must be served under `/lumina-web/**` (for example
`/lumina-web/themes/chat.css`).

`IndexServlet` loads `static/index.html` and injects one `<link rel="stylesheet">` per
`ExtensionRegistry.themeCssResources()` entry after the base `lumina.css` link. Keep theme CSS
limited to Lumina tokens and component classes.

The built-in `io.lumina.web.theme.ChatTheme` ships with Lumina and contributes
`/lumina-web/themes/chat.css` for `PageLayout.CHAT` polish.

## AI and transport

Implement `AiProvider` to supply `TokenStream` instances. The WebSocket transport is the supported
transport today; alternate SSE adapters should preserve the same intent and snapshot semantics.
