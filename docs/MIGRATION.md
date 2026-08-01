# Migrating to Lumina 1.0

The 1.0 API freezes the server-driven `Ui` contract introduced in 0.x.

- Use `ui.pageConfig(...)` before other UI calls.
- Use `ui.navigate("/path")` and `ui.path()` for route-aware apps; clients connect with their path.
- Prefer structured sidebar slots (`brand`, `nav`, `footer`) over freeform chrome.
- AI provider integrations should implement `AiProvider` and return `TokenStream`.

No automatic source migration is required for the 0.8 widget APIs.
