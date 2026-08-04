# Changelog

All notable changes to this project are documented here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Pixel-art creature, weapon and potion sprites, packed into a texture atlas at build time.

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
