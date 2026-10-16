# ADR-0013: Theming
Status: proposed

## Context
The window uses Modena and has no stylesheet. The brief asks for a dark default, a light theme, and one token set. AtlantaFX is the obvious JavaFX theme library. Its README, retrieved 2026-10-14, requires Java 25+ and JavaFX 27+. ADR-0012 recommends staying on Java 21.

## Options
- A project stylesheet that maps `docs/design/TOKENS.md` onto JavaFX looked-up colours. No new dependency. Both themes ship as two stylesheets sharing the same token names.
- AtlantaFX, including Primer, Nord, Cupertino, and Dracula. Current releases do not run on Java 21. An older release might; that has to be measured, and it still adds a dependency the owner must accept.

## Recommendation
Ship the two stylesheets from `docs/design/TOKENS.md`. Dark is the default. E0-T04 checks 100%, 150%, and 200% on Windows, and records whether text still meets the ratios in the token file. It may *look* at an older AtlantaFX build inside `spikes/e0-t04/` and must not add it to a module POM.

A no-go is a contrast or HiDPI failure of the token values. The fix is a token change, not a library, unless the owner first accepts a JDK move.

## Consequences
UI issues reference token names. Raw hex in `ccmm-app` is a review failure. JavaFX regular and bold weights are the only weights in the spec.

## Measured result (E0-T04, 2026-10-16)
`docs/benchmarks/hidpi.md` ends with **go**. Primary and secondary text ratios match TOKENS.md; 24 px tree rows were not clipped at 100/150/200% on Windows for dark and light. AtlantaFX was not added. This ADR stays **proposed**.
