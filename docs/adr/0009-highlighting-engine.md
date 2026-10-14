# ADR-0009: Highlighting engine
Status: proposed

## Context
The editor shows plain text. ADR-0010 recommends opening any text file and highlighting a fixed set of languages. Metrics stay on JavaParser. Highlighting must not run on the JavaFX thread and must not stall a keystroke. Helix issue 15619 (linked from `docs/design/REFERENCE_SURVEY.md`) is a case where a syntax parser did stall insert mode.

## Options
- Per-language regular-expression lexers for a fixed set: Java, JSON, Markdown, XML, `.properties`, and, if ADR-0010 is accepted, JavaScript, TypeScript, and CSS. The lexer runs on a background thread. A newer edit cancels it.
- Eclipse tm4e with TextMate grammars. Quality is higher. The licence is EPL-2.0, which the owner must accept before the dependency is added.
- Tree-sitter through a native binding. Grammars are strong. The binding's JDK and native-library requirements have to be proven on 21, and the parse must stay off the keystroke path.

## Recommendation
Start with cancellable regex lexers for the fixed set. E0-T02 measures highlight time on the same 10,000-line and 5 MB buffers as E0-T01 and writes `docs/benchmarks/highlighting.md`.

Go for regex lexers when a full highlight of the 10,000-line file finishes under 50 ms off the JavaFX thread and a keystroke still meets the 16 ms p95 budget with highlighting enabled. A no-go switches the plan to tm4e if the owner accepts the EPL dependency, or drops highlighting for that language rather than blocking the editor. Tree-sitter is adopted only when the spike shows a JDK 21 build that cannot run on the JavaFX thread and beats the regex times. No dependency is added in the planning PR.

## Consequences
E2-T03 implements whichever engine this ADR names after the spike. Unsupported files open as plain text. A parse or highlight failure never clears the buffer.
