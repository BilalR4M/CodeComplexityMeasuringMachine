# ADR-0007: Cognitive complexity
Status: accepted

## Context
Cognitive complexity (the SonarSource metric) weights nesting and distinguishes `else-if` from a new branch. It is not one of the legacy screens. Cyclomatic complexity, Halstead, maintainability index, LOC, and loop nesting are.

## Options
- Add cognitive complexity in v1 beside cyclomatic complexity.
- Leave it out of v1. The report model can gain a field later without changing the v1 gate rules.

## Recommendation
Post-v1. v1 ships the metrics the legacy app showed, with the formulas in ADR-0004.

## Consequences
`.ccmm.yml` has no cognitive threshold in v1. Adding the metric later is a minor feature, not a change to the accepted stack.
