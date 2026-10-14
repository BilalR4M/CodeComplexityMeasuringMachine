# ADR-0012: JDK and JavaFX version
Status: proposed

## Context
The build is Java 21 (`maven.compiler.release` 21) and JavaFX 21.0.5. ADR-0008's incubating editor and the current AtlantaFX README (Java 25+, JavaFX 27+, retrieved 2026-10-14) both want a newer platform. CI, jpackage, and the accepted ADR-0001 are all on 21.

## Options
- Stay on Java 21 and JavaFX 21.0.5. RichTextFX and a custom stylesheet work. The incubating `CodeArea` and current AtlantaFX do not.
- Move to the current Java LTS and the JavaFX that the chosen editor needs. That is a toolchain change across CI, jpackage, and every module. It is justified only by an editor no-go.

## Recommendation
Stay on Java 21 and JavaFX 21.0.5. E0-T01 may *measure* a newer JavaFX in `spikes/e0-t01/` without changing the parent POM. Adopting that runtime is a separate owner decision after a no-go, not part of the spike.

## Consequences
ADR-0013 does not adopt current AtlantaFX. A future "move JDK" change is ask-first: it touches CI and the release workflow.
