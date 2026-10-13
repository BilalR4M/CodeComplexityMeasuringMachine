# ADR-0004: Metric formulas
Status: accepted

## Context
The legacy web app computes Halstead, cyclomatic complexity, maintainability index, LOC, and loop nesting with regular expressions. Those scans disagree with each other: the Halstead endpoint and the maintainability endpoint use different operator and operand rules, LOC for maintainability is a non-empty line count rather than the line-count endpoint, cyclomatic complexity counts `else`, `return`, and `throw`, and loop depth treats the prefix `do` inside `double` as a loop. See `docs/planning/discovery.md`.

v1 analysis is Java-only, through JavaParser (ADR-0001).

## Options
- Preserve the legacy regex results, including the disagreements above, so old screenshots match.
- Compute one AST-based definition and keep `fixtures/legacy/sampleCode.txt` as an input only. Document the numeric differences; do not assert the regex numbers.

## Recommendation
Use JavaParser.

- Cyclomatic complexity is McCabe per method: start at 1, add 1 for `if`, `for`, `while`, `do`, `case`, `catch`, a conditional `&&` or `||`, and a ternary. Do not add for `else`, `return`, `throw`, or `switch`. The file value is the sum of its methods.
- Halstead operators and operands come from the AST. Keywords are not operands. Numeric and string literals are operands. Vocabulary, length, volume, difficulty, and effort use log base 2. Maintainability index uses this volume, not a second scan.
- Maintainability index is `(171 - 5.2 * ln(volume) - 0.23 * cyclomatic - 16.2 * ln(loc)) * 100 / 171`, clamped at 0. `loc` is the code-line count from the LOC metric.
- LOC reports physical lines, code lines, comment lines, and blank lines. A line that is only a comment or only whitespace is not code.
- Loop nesting is the maximum AST nesting of `for`, `while`, and `do`. An identifier such as `double` does not count.

## Consequences
Golden tests lock the new numbers. A difference from the legacy API is expected and is not a bug. Accepting a different formula later is an ADR change and a golden-fixture update.
