# Editor bake-off

E0-T01. Spike sources live under `spikes/e0-t01/` and are not on the app classpath.
The percentile helper is duplicated from `KeystrokeReport` so the spike stays standalone.

## RichTextFX (`org.fxmisc.richtext.CodeArea`)

- OS: Windows 11 amd64
- JavaFX: 21.0.5
- Command: `mvn -f spikes/e0-t01 -q exec:java`
- Buffer (keystroke): 10000 lines
- Samples: 200 single-character inserts
- p95: 1.802 ms
- memory after 10k-line load: 367.2 MB committed
- Buffer (large): 5 MB text
- memory after 5 MB load: 458.5 MB committed
- scroll: no ScrollBar found; paragraphs=109267
- gutter chip / side decoration: yes
- caret/selection without moving viewport (local range at top): yes
- folding feasible: no
  RichTextFX 0.11.2 exposes no fold API. Folding is not required for a go.

## Incubating JavaFX `CodeArea`

- JavaFX: not measured on this run
- Reason: `jfx.incubator.scene.control.richtext.CodeArea` is not on the JavaFX 21.0.5 classpath used by this repository (ADR-0012 recommends staying on 21). The incubating module appears in JavaFX 25/26 and may change or be removed. No side toolchain was applied to the parent POM.
- Result: skip. RichTextFX remains the measured candidate.

## Decision

go

RichTextFX meets the go thresholds. Keep ADR-0001. Folding stays optional and is currently no.

## Product path (E2-T10)

Folding is **won't-do** for v1. `folding feasible: no` above; RichTextFX 0.11.2 has no fold API. Issue #78 closes with this citation and ships no fold controls. Revisit only if ADR-0008 (or a later editor ADR) adopts a component that exposes folding.
