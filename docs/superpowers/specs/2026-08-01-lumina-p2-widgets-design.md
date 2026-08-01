# Lumina P2 — Remaining Core Widgets

**Date:** 2026-08-01  
**Version target:** `0.8.0-SNAPSHOT`  
**UX:** Enterprise constitution + hard-reset shell  

## Goal

Ship the remaining Streamlit-class widgets: checkbox, number input, selectbox, radio, slider, spinner, download button.

## Author API

```java
boolean checkbox(String label);
boolean checkbox(String label, boolean value);

double numberInput(String label);
double numberInput(String label, double value);
double numberInput(String label, double value, double min, double max, double step);

String selectbox(String label, List<String> options);
String selectbox(String label, List<String> options, int index);

String radio(String label, List<String> options);
String radio(String label, List<String> options, int index);

double slider(String label, double min, double max);
double slider(String label, double min, double max, double value);
double slider(String label, double min, double max, double value, double step);

void spinner(String label, Runnable body);

boolean downloadButton(String label, byte[] data, String fileName);
```

## Wire / state

| Type | Intent | State |
|------|--------|-------|
| checkbox, number_input, selectbox, radio, slider | `input` | WidgetState |
| download_button | `click` | consumeClick |
| spinner | none | interim flush then remove |

- Empty options → `LuminaException`
- Download ≤ 1 MiB (same as upload)
- Slider: client local readout on `input`, intent on `change`
- Spinner: add node → flushBefore → body → omit from final tree

## Client

Custom elements + CSS per constitution (labels, focus-visible, ≥40px targets, fieldset for radio).

## Showcase

Add Widgets page (`/widgets`) demonstrating all new controls.
