# Changelog

All notable changes to this project are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Pixel-art creature, weapon and potion sprites, packed into a texture atlas at build time.
- **Every screen rebuilt on the pixel kit** — title, new game, THE LEDGER, TROPHIES, the run-end
  panel and the guided tutorial's overlay, all drawn in immediate mode at a fixed 1280×720 in
  Silkscreen. With the last two, **Scene2D is gone from the project entirely**, and with it the
  IM Fell English and Alegreya Sans faces.
- The run-end panel covers both outcomes in one layout, the death swapping the gold accents for
  dried blood; it shrinks when a run unlocked no trophies rather than leaving a gap.
- The tutorial's callout grows to fit its narration, points at the card being taught with a
  stepped notch, and rings it with eight corner ticks instead of a closed frame.
- **Menu buttons act on release rather than press**, and only where the press began, so a
  press can be taken back by sliding off — including `ERASE EVERYTHING` on the destructive
  confirmation. A held plate is drawn pressed: the bevel inverts, the face drops to its
  shadowed step and the label travels into the recess. Cards keep press-to-act.
- A modal overlay now dims the screen behind it with a 4×4 ordered dither, matching the death
  wipe rather than adding an alpha scrim. Two strengths: full for a dialog that must be
  answered, half for the tutorial, where the board underneath is what you are about to click.
- The first-run "New here?" prompt gets that dim, and its own layout — it had been borrowing the
  menu column's button positions, which put a plate straight through its second line of copy.

### Changed
- THE LEDGER's totals panel reports figures the game actually keeps. The reference render asked
  for "weapons broken"; weapons degrade in Scoundrel and never break.
- TROPHIES rows are taller and their descriptions wrap, so the real achievement copy fits at a
  legible size.
- The run-end panel's middle figure no longer repeats the score. Clearing the dungeon scores
  exactly the health you kept, so a win now reports `DAMAGE TAKEN` — what getting out cost — in
  the health bar's own blood; a death, which has no health left to report, counts the monsters
  `STILL DOWN THERE`, the one figure that explains how far below zero the score is.
- `CLEARED` and the score under it are green on the run-end panel, the same green the ledger
  sets a cleared run in. The reference render has the headline cream.
- **The sprites breathe.** Every card's art rises and settles ±2px on one shared clock — eight
  steps at the idle 6fps — while the card frame and its printing hold still. The whole room moves
  as one, and the hovered creature's five-frame idle plays on top of it.
- Time is floored onto the frame grid in one place (`Frames`) rather than nine. Nothing in this
  art tweens — effects hold at 12 fps and idles at 6 — and the epsilon that keeps a frame boundary
  from landing a tick late now lives once, with the note explaining it, instead of being copied
  beside eight of the nine floors that needed it.
- Nothing on the converted screens is set below 12px. Silkscreen strokes are 1px, and at the
  ×1.5 scale a 1920×1080 display gets, an 8px glyph loses half of them to rounding.

### Removed
- Scene2D, in full: the stage, the `Widgets` button styles, and `Theme`'s drawable helpers.
- The IM Fell English and Alegreya Sans faces, now that nothing sets type in them.

## [1.0.0] - 2026-08-05

First complete release: the full base game, playable start to finish.

### Added
- **Rules engine** — the complete Scoundrel ruleset as pure, headless Java: room dealing,
  avoiding (and the never-twice-in-a-row restriction), bare-handed and armed combat, weapon
  degradation, the one-potion-per-turn cap, and both scoring branches including the
  cap-plus-final-potion case. No libGDX dependency; playable to completion in a unit test.
- **Scene2D interface** — framed card tiles, press-to-resolve with a chooser for cards that
  allow more than one move, an equipped-weapon rail, a fading event feed and an end-of-run panel.
- **Motion and effects** — cards deal in and sweep away on avoid, HP damage and heal pulses,
  per-card resolve effects (bare-handed strike, weapon kill, potion pour) and a death cinematic.
- **Atmosphere** — a procedural torchlit backdrop with live flicker and drifting embers.
- **High scores and statistics** — finished runs are recorded to `~/.scoundrel/runs.log`; THE
  LEDGER screen shows the top runs and lifetime totals, ranked separately per difficulty.
- **Achievements** — ten trophies evaluated from the engine's event stream and persisted with an
  unlocked latch, shown on a TROPHIES screen. Earned in Standard only.
- **Difficulty modes** — Standard, Relentless (avoiding forbidden) and Frail (14 health, healing
  capped there), implemented as alternate rulesets rather than engine branches.
- **Guided tutorial** — a scripted first run that teaches every rule, including both halves of
  scoring, offered once on first launch and replayable from "How to play".
- **Fullscreen** — borderless-fullscreen by default, toggled with F11 or Alt+Enter.
- **Crash reporting** — uncaught exceptions are appended to `~/.scoundrel/crash.log` with a
  timestamp and stack trace.
- **Self-contained builds** — Windows x64 and macOS Apple Silicon archives that bundle a trimmed
  JDK, so no Java installation is needed to play.
- **Continuous integration** — tests and a JaCoCo coverage gate (90% line / 75% branch on every
  pure package) run on every push and pull request.

### Known limitations
- The macOS build is unsigned and has not been launched on a Mac; the Windows build is unsigned,
  so both show a first-launch warning.
- Cards are drawn as typed tiles with rank and suit. Illustrated sprites are in progress.
- There is no mid-game save or resume — a run is a single sitting.

[Unreleased]: https://github.com/tomerba6/scoundrel-game/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/tomerba6/scoundrel-game/releases/tag/v1.0.0
