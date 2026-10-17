# Highlighting bake-off

E0-T02. Spike sources live under `spikes/e0-t02/` and are not on the app classpath.

## Regex Java lexer

- OS: Windows 11 amd64
- JDK: 25.0.3
- Command: `mvn -f spikes/e0-t02 package exec:java`
- Buffer (highlight): 10000 lines
- Highlight time (10k, off FX thread): 24.220 ms
- Highlight time (5 MB, off FX thread): 244.973 ms
- Keystroke p95 with highlighting enabled: 0.984 ms
- Cancel / latest-wins discards stale spans: yes
- Budget: highlight 10k under 50 ms; keystroke p95 under 16 ms

## tm4e

- tm4e: not measured
- Reason: `org.eclipse.tm4e` is not a spike dependency and must not enter `ccmm-app` without an EPL-2.0 acceptance. No side toolchain was applied to the parent POM.
- Licence note: EPL-2.0
- Result: skip. Regex remains the measured candidate.

## Tree-sitter

- tree-sitter: not measured
- JDK: 25.0.3
- Reason: no tree-sitter Java binding was on the spike classpath. Common candidates were absent; native libraries were not loaded.
- Result: skip. Do not block the regex path.

## Decision

go

Regex lexers meet the go thresholds. Keep ADR-0009's recommendation. Do not add tm4e or tree-sitter to `ccmm-app` from this spike.

## Product path (E2-T03)

`RegexJavaHighlighter` + `HighlightScheduler` live in `ccmm-app` and paint the open Java buffer via RichTextFX style classes. Lexer work stays off the FX thread; style paint is one FX pass and is skipped when there are no tokens or brackets.

- OS: Windows 11 amd64
- Command: `CCMM_KEYSTROKE=true mvn -B -ntp -pl ccmm-app -am test -Dtest=KeystrokeLatencyHarness,KeystrokeReportTest`
- Buffer: 10000 lines (editor harness after highlight landed)
- Keystroke p95: 3.163 ms (see `docs/benchmarks/keystroke-latency.md` Editor section)
- Spike keystroke p95 with highlighting on: 0.984 ms
- Budget: under 16 ms

go
