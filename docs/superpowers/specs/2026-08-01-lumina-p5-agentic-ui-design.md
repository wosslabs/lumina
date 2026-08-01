# Lumina P5 — Agentic UI

**Date:** 2026-08-01  
**Version target:** `0.10.0-SNAPSHOT` (ships with P4)

## APIs

```java
void agentTimeline(List<Map<String, Object>> steps); // id, label, status, detail
void toolInvocation(String toolName, String status, String detail);
boolean approval(String prompt); // button row Approve/Reject → boolean this run via widget state
void memoryPanel(List<Map<String, Object>> entries);
```

## Behavior

- Display-only timeline/tool/memory panels
- `approval` uses click consume pattern (two buttons or one approve + one reject keyed)

## Demo

`AgentDemoApp` in examples.
