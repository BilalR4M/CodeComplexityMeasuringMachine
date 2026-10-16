# HiDPI theme tokens

E0-T04. Spike sources live under `spikes/e0-t04/`. Stylesheets use looked-up `-fx-ccmm-*` colours; scene graph code has no hex literals.

- OS: Windows 11 amd64
- Command: `mvn -f spikes/e0-t04 package exec:java`
- Scales: 100%, 150%, 200% (Scale transform on the scene root)
- Tree row height: 24 px

## Contrast (TOKENS.md)

- dark text-primary: 13.61:1 (TOKENS.md 13.61:1) ok
- dark text-secondary: 7.69:1 (TOKENS.md 7.69:1) ok
- light text-primary: 16.67:1 (TOKENS.md 16.67:1) ok
- light text-secondary: 8.86:1 (TOKENS.md 8.86:1) ok

## Captures

- dark @100: hidpi-dark-100.png; tree row 24 px not clipped
- dark @150: hidpi-dark-150.png; tree row 24 px not clipped
- dark @200: hidpi-dark-200.png; tree row 24 px not clipped
- light @100: hidpi-light-100.png; tree row 24 px not clipped
- light @150: hidpi-light-150.png; tree row 24 px not clipped
- light @200: hidpi-light-200.png; tree row 24 px not clipped

## Decision

go

Primary and secondary text match the TOKENS.md ratios. Tree rows are not clipped at 100/150/200%. AtlantaFX was not added.
