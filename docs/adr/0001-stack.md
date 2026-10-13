# ADR-0001: Java 21, JavaFX + RichTextFX, Maven multi-module
Status: accepted
Context: Desktop IDE written in Java for Windows/Linux/macOS; project size in the hundreds of files.
Decision: JavaFX UI with RichTextFX editor; JavaParser for Java-only analysis; metrics as an in-process library shared by IDE and CLI; Maven for build; jpackage for installers.
Consequences: no HTTP metrics service; per-OS builds required for JavaFX natives; RichTextFX large-file behaviour must be proven in Phase 0 (S1).
