# E0-T05 headless UI tests

Standalone spike notes. Does not change `.github/workflows/ci.yml` until ADR-0015 is accepted.

## Finding

- `windows-latest` and `macos-latest`: `IdeWindowTest` already runs (display present).
- `ubuntu-latest` without a framebuffer: all 6 methods skip via `GraphicsEnvironment.isHeadless()`.
- Harness: **Xvfb** via `xvfb-run`. Package: `xvfb` (preinstalled on `ubuntu-latest` GitHub runners; otherwise `sudo apt-get install -y xvfb`).

## Command (Linux)

```bash
xvfb-run -a mvn -B -ntp -pl ccmm-app -am test -Dtest=IdeWindowTest -Dsurefire.failIfNoSpecifiedTests=false
```

Monocle was not required. TestFX was not added.

## Proof

Spike workflow run [38936761522](https://github.com/BilalR4M/CodeComplexityMeasuringMachine/actions/runs/38936761522): under `xvfb-run`, `IdeWindowTest` reported `Tests run: 6, Failures: 0, Errors: 0, Skipped: 0`.

## After ADR-0015 is accepted

Change the Ubuntu `ci.yml` step from `mvn -B -ntp verify` to:

```yaml
- run: xvfb-run -a mvn -B -ntp verify
  if: runner.os == 'Linux'
- run: mvn -B -ntp verify
  if: runner.os != 'Linux'
```

That is ask-first. This spike does not land that edit.
