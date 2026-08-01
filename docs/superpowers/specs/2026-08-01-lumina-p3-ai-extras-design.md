# Lumina P3 — AI Component Extras

**Date:** 2026-08-01  
**Version target:** `0.9.0-SNAPSHOT`

## Goal

Add first-class AI metadata widgets on top of existing chat/streaming.

## APIs

```java
void citation(String title, String urlOrRef, String snippet);
void ragSources(List<Map<String, Object>> sources); // title, uri, score?
void toolCall(String name, String status, Object input, Object output);
void usage(long promptTokens, long completionTokens, Double costUsd, Long latencyMs);
```

## Wire types

`citation`, `rag_sources`, `tool_call`, `usage` — display-only (no intents).

## Client

Cards/panels matching enterprise tokens; tool_call shows name + status badge; usage as compact metrics row.

## Showcase

Extend About or add `/ai` page with sample citation/tool/usage.
