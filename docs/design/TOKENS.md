# Design tokens

Dark is the default. Light is the alternate. UI code references these names and never a hex literal. JavaFX font weights are regular and bold only.

Spacing: 4, 8, 12, 16, 24. Radius: 4 (controls), 6 (panels). Tree row 24, tab 28, status 24, activity icon 16. Hit target at least 24. Window minimum 900×600. Default 1280×800. The editor column is at least 65% when both side regions are open at 1280 px; below 1100 px the inspector starts collapsed.

## Colour

Contrast ratios were computed on 2026-10-14 with the WCAG relative-luminance formula. Text tokens are at least 4.5:1 on `surface-0`. Remote swatches are at least 3:1 against both `surface-0` values (non-text). Chip labels use the named foreground and are at least 4.5:1 on the swatch.

### Dark

| Token | Value | Contrast on surface-0 |
|---|---|---|
| `surface-0` | `#1E1E1E` | — |
| `surface-1` | `#252526` | chrome, side bar |
| `surface-2` | `#2D2D30` | raised panel |
| `surface-3` | `#3C3C3C` | hover |
| `text-primary` | `#E8E8E8` | 13.61:1 |
| `text-secondary` | `#B0B0B0` | 7.69:1 |
| `text-disabled` | `#8A8A8A` | 4.83:1 |
| `accent` | `#4CC2FF` | 8.31:1 |
| `success` | `#3DDC97` | 9.43:1 |
| `warn` | `#E6B450` | 8.74:1 |
| `danger` | `#F07178` | 5.83:1 |
| `selection` | `#264F78` | editor selection fill |
| `diff-add` | `#1F4D32` | added line fill |
| `diff-remove` | `#6F2C33` | removed line fill |
| `text-on-remote-light` | `#FFFFFF` | label on a dark swatch |
| `text-on-remote-dark` | `#1A1A1A` | label on a light swatch |

### Light

| Token | Value | Contrast on surface-0 |
|---|---|---|
| `surface-0` | `#FFFFFF` | — |
| `surface-1` | `#F3F3F3` | chrome |
| `surface-2` | `#E8E8E8` | raised panel |
| `surface-3` | `#DCDCDC` | hover |
| `text-primary` | `#1E1E1E` | 16.67:1 |
| `text-secondary` | `#4A4A4A` | 8.86:1 |
| `text-disabled` | `#767676` | 4.54:1 |
| `accent` | `#005A9E` | 7.10:1 |
| `success` | `#0F7B3A` | 5.36:1 |
| `warn` | `#8A5A00` | 5.93:1 |
| `danger` | `#B42318` | 6.57:1 |
| `selection` | `#C7E0F4` | editor selection fill |
| `diff-add` | `#D8F3DC` | added line fill |
| `diff-remove` | `#F8D7DA` | removed line fill |

`text-disabled` is for inactive controls only. Essential information uses `text-secondary` or stronger.

### Metric bands

Bands come from `.ccmm.yml` (`cyclomatic` and `nesting` maximums, `maintainability` minimum). Missing file: cyclomatic 10, nesting 3, maintainability 20, matching the CLI example in `docs/architecture.md`.

| Token | When |
|---|---|
| `success` | under every maximum and at or above the maintainability minimum |
| `warn` | within 20% of a failing threshold, or failing exactly one rule |
| `danger` | failing a rule by more than that margin |

A stale result uses the same band at 50% opacity plus the word `stale`.

### Remote colours

Eight swatches, also distinguished by the name flag. Chip foreground is the token in the last column.

| Token | Value | On dark surface-0 | On light surface-0 | Chip text |
|---|---|---|---|---|
| `remote-1` | `#0072B2` | 3.21:1 | 5.19:1 | `text-on-remote-light` (5.19:1) |
| `remote-2` | `#D55E00` | 4.31:1 | 3.87:1 | `text-on-remote-dark` (4.50:1) |
| `remote-3` | `#009E73` | 4.87:1 | 3.42:1 | `text-on-remote-dark` (5.09:1) |
| `remote-4` | `#CC79A7` | 5.45:1 | 3.06:1 | `text-on-remote-dark` (5.69:1) |
| `remote-5` | `#999933` | 5.52:1 | 3.02:1 | `text-on-remote-dark` (5.77:1) |
| `remote-6` | `#CC6677` | 4.55:1 | 3.66:1 | `text-on-remote-dark` (4.75:1) |
| `remote-7` | `#2E7D4F` | 3.30:1 | 5.05:1 | `text-on-remote-light` (5.05:1) |
| `remote-8` | `#4E6E8A` | 3.11:1 | 5.36:1 | `text-on-remote-light` (5.36:1) |

Assign the lowest free index as a peer joins. Reuse an index only after that peer has left.

## Type

- UI: the platform font, 12 px, regular. Section titles 12 px bold.
- Editor: JetBrains Mono, SIL OFL 1.1, 13 px regular. Ligatures off unless the user turns them on. See ADR-0014.
- Line height in the editor: 1.4.

## Motion

No animation longer than 120 ms. Remote carets do not animate. One toast at a time for a join/leave burst.
