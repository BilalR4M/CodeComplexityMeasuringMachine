# ADR-0014: Icon set and editor font
Status: proposed

## Context
The tree cells draw a name and nothing else. The brief asks for one licensed icon set and a bundled OFL monospace, with ligatures optional. JavaFX UI chrome can use the platform font.

## Options
- Editor font: JetBrains Mono, SIL Open Font License 1.1 ([JetBrains Mono](https://www.jetbrains.com/lp/mono/), retrieved 2026-10-14). It can be bundled. Ligatures are a font feature the user can turn off.
- Another OFL face (for example a no-ligature face). Same obligations: ship the licence.
- Icons: Lucide. The project licence is ISC, and the repository `LICENSE` also carries the MIT notice for the Feather icons it derived ([lucide LICENSE](https://github.com/lucide-icons/lucide/blob/main/LICENSE), retrieved 2026-10-14). Ship that whole file, not a one-line credit. Enough glyphs for chrome (folder, file, search, warning, comment). File types map to a small set of those glyphs, not a separate icon theme.
- A file-type icon theme copied from another editor. Those themes are often not licensed for bundling. Rejected.

## Recommendation
Bundle JetBrains Mono with its OFL text under `ccmm-app/src/main/resources/fonts/`, and vendor the Lucide SVGs the UI actually uses plus the upstream `LICENSE` (ISC and the Feather MIT notice) under `ccmm-app/src/main/resources/icons/`. The implementing issues (E1-T08, E4-T03) do not add either until the owner accepts this ADR. No dependency is added in the planning PR.

The extension map is: `.java` file-code, `.json` file-json, `.md` file-text, `.xml` file-code, `.css` `.js` `.ts` `.tsx` file-code, anything else file, directories folder. Git state is colour, not a second icon.

## Consequences
The OFL text and the Lucide `LICENSE` ship in the installer. Ligatures default off because not every platform renders them the same way.
