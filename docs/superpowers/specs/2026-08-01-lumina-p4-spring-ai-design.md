# Lumina P4 — Spring AI & Provider SPI

**Date:** 2026-08-01  
**Version target:** `0.10.0-SNAPSHOT`

## Goal

First-class bridge from Spring AI `ChatClient` / streaming to `ui.ai(TokenStream)`, plus a provider SPI for non-Spring use.

## Deliverables

1. `AiProvider` SPI in `lumina-core` or `lumina-spring-ai`: `TokenStream stream(String prompt)`
2. `SpringAiChatClientProvider` adapter wrapping Spring AI ChatClient when on classpath
3. `application` properties: `lumina.ai.provider=echo|openai|ollama` (echo default)
4. Document how to call `ui.ai(provider.stream(prompt))`
5. Keep echo working without API keys

## Non-goals

Full multi-vendor SDK matrix in-tree; Azure/Bedrock/Vertex as documented extension points only in this pass.
