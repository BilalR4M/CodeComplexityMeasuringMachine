# ADR-0008: Editor component
Status: proposed

## Context
ADR-0001 accepted RichTextFX. The keystroke spike on a 10,000-line buffer is a `go` (p95 1.245 ms, committed memory 302.6 MB in `docs/benchmarks/keystroke-latency.md`). The IDE still has no folding, no gutter chips, and no measurement on a 5 MB file. JavaFX 25 and 26 ship an incubating `CodeArea` / `RichTextArea` in `jfx.incubator.richtext`. That module's own summary says the API can be removed. This repo is Java 21 and JavaFX 21.0.5.

## Options
- Stay on RichTextFX `org.fxmisc.richtext.CodeArea`. It meets the keystroke budget. Gutter chips and remote carets have to be built as side decorations. Folding is not provided.
- Move to the incubating JavaFX `CodeArea`. It is virtualized and has a caret model. `leftDecorator` is already used for line numbers, so gutter chips need another decoration path. It requires a newer JavaFX than ADR-0012 recommends, and the API is incubating.
- A third embeddable component, only if both of the above miss a go threshold. A separate toolkit such as Lapce is not embeddable in this scene and is not a candidate.

## Recommendation
Keep RichTextFX until E0-T01 records the bake-off. Do not change ADR-0001 in this planning PR. The spike compares the two components on a 10,000-line file and a 5 MB file and writes `docs/benchmarks/editor-bakeoff.md`.

Go stays with RichTextFX when all of these hold: keystroke p95 under 16 ms, committed memory of the IDE process under 500 MB on the 5 MB file, a caret and selection API that can draw a remote caret without moving the local viewport, and a side decoration that can show a gutter chip. Folding is recorded as feasible or not; it is not required for a go.

A no-go names the replacement, blocks the E2 tasks that assume RichTextFX, and does not implement the loser. Switching to the incubating control also requires the owner to accept a JDK change (ADR-0012).

## Consequences
E2 editor behaviour is specified against a decoration API in `docs/architecture.md`, not against RichTextFX types, so a no-go does not rewrite the UX. Spike code lives under `spikes/e0-t01/` and is not shipped.
