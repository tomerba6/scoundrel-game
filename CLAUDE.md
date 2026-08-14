# Scoundrel — Project Memory

A desktop implementation of **Scoundrel**, a single-player roguelike card game, built with LibGDX.

## Tech stack
- Java (language level 21), Gradle multi-module project.
- LibGDX with the **LWJGL3** desktop backend only. No Android/iOS/web modules.
- UI built with Scene2D (`scene2d.ui`).

## Commands
- Run the game: `./gradlew lwjgl3:run` (Windows: `gradlew.bat lwjgl3:run`).
- Run tests: `./gradlew core:test`. Tests use JUnit 5 and live in `core/src/test/java`.
  One class: `./gradlew core:test --tests '*FramesTest'`.
- Tests + coverage gate: `./gradlew core:check`. JaCoCo enforces a minimum
  coverage on each **pure** package (`model`/`rules`/`runs`/`achievements`/
  `tutorial`); the GL-bound `screens` layer is excluded (screenshot-verified,
  not gated). HTML report at `core/build/reports/jacoco/test/html/`.
- Drive/screenshot the real game (the only way to verify UI): the
  `run-scoundrel` skill (`.claude/skills/run-scoundrel/`).
- `assets/assets.txt` is regenerated from the directory listing by `generateAssetList`
  (wired to `processResources`) — never hand-edit it, and note that anything left in
  `assets/` ships whether or not any code loads it.

## Architecture — these are hard rules
- **All game logic lives in the `core` module.** `lwjgl3` is a thin launcher only; do not
  put game logic there.
- **The rules engine MUST be pure Java with no LibGDX imports.** No `com.badlogic.gdx.*`
  anywhere in the rules/model code. It is plain, deterministic, headless logic.
- Player actions are modeled as functions that take the current game state and return a new
  state (or mutate a well-encapsulated state object). They must be unit-testable without a
  window, a render loop, or any graphics.
- The UI layer (Scene2D screens) only does two things: draw the current state, and translate
  user input into calls on the rules engine. It never contains rule logic.
- Suggested package split inside `core`:
    - `...scoundrel.model` — cards, deck, game state (pure).
    - `...scoundrel.rules` — actions and rule resolution (pure).
    - `...scoundrel.screens` — the screens (LibGDX-dependent). **All immediate-mode
      now: there is no Scene2D left in the project, and Silkscreen is the only
      face.** Pure logic is
      kept **out** of the GL classes: leaf helpers are extracted into small,
      headlessly-unit-tested classes (`RoomMotion`, `CardHitRegions`, `ClockText`,
      `FeedText`, `Labels`, `ResolveEffect`, `TorchFlicker`, `Embers`,
      `PressGesture`, `LedgerRow`, `LedgerTotals`, `TrophyEntry`, `TextWrap`,
      `EndSummary`, `ButtonRow`, `CalloutPlacement`, `CornerTicks`, `Frames`,
      `SpriteBob`), leaving the
      screens as thin views verified by screenshot. `TextWrap` and `ButtonRow` are
      the pattern to copy when a helper needs a font: the measuring is passed in
      as a function, so the arithmetic stays testable. When touching a screen, prefer
      extracting any new pure formatter/decision/geometry the same way — write a
      characterization test first, then move the method verbatim.
    - `...scoundrel.CrashLog` — appends uncaught crashes to `~/.scoundrel/crash.log`
      (installed by the launcher); pure and tested, robust (never throws itself).
    - `...scoundrel.runs` — run recording + local high-score persistence (pure Java;
      observes the engine from outside — `model`/`rules` never import it).
    - `...scoundrel.achievements` — achievement definitions, evaluation, and the
      unlocked-latch persistence (pure Java; observes the engine + run log from
      outside — `model`/`rules`/`runs` never import it).
    - `...scoundrel.tutorial` — the guided first-run: a scripted deck + narrated
      steps and the gating state machine, plus the tutorial-seen flag (pure Java;
      drives the engine through its public ordered-deck entry — `model`/`rules`
      never import it). The Scene2D tutorial *mode* lives in `screens.GameScreen`.
- **Detailed design reference:** the full rules-engine design — the `model`/`rules`
  types, the turn loop, extension seams, and the locked edge-case decisions — is
  documented in [`docs/design.md`](docs/design.md) (prose + Mermaid diagrams).
  Consult it when working on the engine, and keep it in sync when the design changes.
- **UI layer reference:** the UI — locked interview decisions, theme tokens,
  architecture, and every on-screen component — is documented in
  [`docs/ui.md`](docs/ui.md). The board is a torchlit dungeon — a procedural
  backdrop (glow, live flicker, drifting embers) and framed cards on the ramp
  system below. Motion (deal-in, avoid sweep, per-card effects, HP pulses) and
  the atmosphere already ship, now as `BoardView` over `CardFlight` / `HpPulse`
  and the rest — `Choreographer` and `Motion` went with the pixel conversion.
  Consult it when working on screens, and keep it in sync when the UI changes.
- **Sprite art reference:** the pixel art is **finished and delivered** — see
  [`HANDOFF.md`](HANDOFF.md), which is the contract, and the section below for the
  hard rules. The *Ashen* palette is superseded by the 80-colour ramp system it
  defines.

## Sprite art — these are hard rules

The art is **done**: 31 objects (13 creatures × 2 suits, 9 weapons, 9 potions) and 130 idle
frames, all 64×64. [`HANDOFF.md`](HANDOFF.md) is the full contract — region names, palette,
geometry, effect timings, and a 12-step order of work with a verify line per step. Read it
before touching anything visual.

- **Never regenerate, recolour, or "improve" a sprite.** Sprite pixels sit on the locked
  80-colour ramp system (`Ramps`) that took ~30 generations to settle. A helpful palette tweak
  is a regression. If a sprite genuinely needs changing, say so and stop — it is an art task,
  not a code task.
- **The palette has two tiers, and both are tested.** `Ramps` is the 80 and governs sprite
  pixels; `UiPalette` is the 32 colours drawn in code that are not on a ramp — the §11 chrome,
  the bottle, the cleave faces, the HUD tints — all sampled from the reference render, not
  invented. `UiPaletteTest` scans both declaration forms (`static final int … = 0x…` and
  `Color.valueOf("…")`) and fails on a colour in neither. A new colour goes in `UiPalette` with
  a comment saying what it draws.
- **Build the atlas from `art-source/atlas/` only.** `art-reference/sprites/` holds the same
  PNGs under dot-separated names for the HTML mock; it is not the delivery set.
- **Load sprites through the atlas, never as loose files.** The 174 source PNGs sit outside
  `assets/` deliberately, so the only thing on the asset path is the packed
  `assets/sprites/sprites.atlas` (built by the root `packAtlas` task, gitignored). A stray
  `Gdx.files.internal(...)` on a sprite PNG would otherwise succeed and return a
  `Linear`-filtered texture — blurry art, silently. This deviates from `HANDOFF.md` §2, which
  is noted there.
- **Region names are the contract:** `creature_<value>_<name>_<suit>`, frames add
  `_idle_1`…`_idle_5`. Lowercase `[a-z0-9_]`, index last, so
  `atlas.findRegions(stem + "_idle")` returns the five in order. Value is zero-padded
  (`02`–`10`, `11`=J, `12`=Q, `13`=K, `14`=A).
- **`TextureFilter.Nearest`, integer scales only (1, 2, 3, 4), whole-pixel positions.** These are
  hand-placed pixels; a fractional scale or a sub-pixel offset invents colours outside the
  palette and makes the art shimmer. `Math.round` every computed position before drawing.
- **`PixelViewport(1280, 720)`** on every screen. Every number in `HANDOFF.md` is in that space;
  sprites draw at ×2 = 128px inside a 176×256 card. Do not re-derive the layout per window size.
  It is a `FitViewport` that snaps the scale **down to a multiple of 0.5** and letterboxes the
  rest — a plain fit gives ×1.25 at 1600×900, which puts 2.5 screen pixels on each source pixel
  and makes the art crawl. Half-steps, not whole: 1920×1080 fits at exactly ×1.5, already clean
  at ×2 sprites, and integer snapping would letterbox away a third of it. The maths is the pure,
  unit-tested `PixelScale`; keep it there rather than in the GL class.
- **Idles run at 6 fps, effects at 12 fps**, and nothing tweens or rotates — every segment holds
  on a frame. A rotated pixel is a blurred pixel. Floor time through `Frames`
  (`at`/`atPeriod`/`snap`), never a local `1f / 12f`: it carries the epsilon a frame boundary
  needs, and multiplies by the rate so an hour-old clock has not drifted past it.
- **The barehanded stars are four bars, never a rotation** (§10). The diagonal arms are staircases
  of 8×8 blocks — exactly 45°, grid-aligned, no transform. A rotated rect is the worst thing you
  can do to a pixel sprite.
- **Every screen is in scope, not just the board** (§11) — title, new game, ledger, trophies,
  tutorial and run end all move onto the same grammar. They are assembled from five parts
  (frame, face, bevel, label, rule); learn those and the screens are assembly work.
- **Screen transitions are cuts.** One frame, old screen gone, new screen up. No fades, no hover
  glows, no panel shadows, no rounded corners anywhere.
- **The torchlit backdrop stays smooth, and that is now decided** — a soft glow, a continuous
  flicker and sub-pixel drifting embers behind flat-palette sprites. It is the one place that
  breaks the whole-pixel and quantised-timing rules, deliberately: light is not an object. Don't
  dither it, coarse-render it or round the embers to the grid.
- **Hurt and rim frames are generated in Java at load** from each base sprite (§8), not shipped.
  There are no death frames — the card dissolve covers it.
- **Silkscreen replaces IM Fell English and Alegreya Sans entirely**, at pixel-aligned sizes with
  no anti-aliasing. `Theme` loads the two Silkscreen TTFs and nothing else; the three vector
  faces and the Scene2D skin were deleted with the last of Scene2D.
- The nine 16×16 rail icons the brief asked for are **dropped**; the rail shows the card sprite
  at ×1 and that is the finished answer, not a placeholder. Don't generate them.

The mock (`art-reference/Scoundrel - Sprite Directions.dc.html`) is the visual target: the board
at 1280×720, every effect in isolation, all 26 idle cycles, and all six screens. It opens in a
browser — ask the user to compare against it; you can't render it yourself.

## Working preferences
- For any non-trivial change, propose a plan first and wait for review before coding.
- **Tests first (red-green).** For pure logic, write the failing test, run it to confirm it
  fails, then implement to green — not code-then-tests. The `model`/`rules`/`runs`/
  `achievements`/`tutorial` code is pure and headless, so TDD applies throughout. UI
  rendering, which can't be unit-tested, is the exception: verify it by screenshot instead.
  Cover the tricky rules below especially, and get tests green before touching the UI.
- **A UI state you cannot drive to is verified by rigging, not by playing it out.** The run-end
  panel needs a finished run, and finishing one appends to the player's real
  `~/.scoundrel/runs.log`. Point an already-reachable screen at the component with rigged
  values, screenshot it, then `git checkout --` the file — the change is never committed.
- Keep commits small and focused; commit after each working piece.
- **Verify, don't recall — and never quote a number you did not just measure.**
  The docs in this repo *lag the code by design*: `README.md`, `docs/ui.md` and
  `HANDOFF.md` each hold a mix of what shipped, what was replaced, and what was only
  ever planned — both `README.md` and the `run-scoundrel` skill have been caught this
  way, one listing the finished sprites as in progress, the other quoting a test count
  a third of the real one. A figure in a doc was true when written and is evidence of
  nothing today. If a claim is checkable by reading a file or running a command, check
  it **before** stating it — the check is seconds and being wrong costs
  the user their trust in everything else you said.
    - test count → `grep -rhoE "@Test" core/src/test/java --include=*.java | wc -l`
    - coverage → `./gradlew core:test` (it refreshes the report), then parse
      `core/build/reports/jacoco/test/jacocoTestReport.xml` per package. Read the
      `screens` package as two halves — the extracted pure helpers *stay in that
      package*, so its number climbs as the GL classes are hollowed out, and the
      pure-vs-GL split is the meaningful reading rather than the aggregate.
    - purity boundary → `grep -rl 'com.badlogic.gdx' core/src/main/java/com/tomer/scoundrel/{model,rules,runs,achievements,tutorial}` must be empty
    - what a class or screen does → open it. Its entry in `docs/ui.md` may describe the
      version it replaced.
    - runtime behaviour (window size, viewport scale, input routing) → log it from
      inside the running game and read the log. Do not reason about what the platform
      "should" do, and do not trust a screenshot to prove it — see the fullscreen-capture
      traps in `.claude/skills/run-scoundrel/SKILL.md`.
  When a doc figure turns out to be stale, fix the doc in the same change rather than
  working around it.

---

## Game rules (the spec — implement exactly)

### Deck (44 cards)
Start from a standard 52-card deck plus 2 jokers, then remove: both jokers, the red face
cards (J/Q/K of hearts and diamonds), and the red aces (A of hearts, A of diamonds). What
remains:
- **Monsters** — all 26 clubs and spades.
- **Weapons** — the 9 diamonds (2 through 10).
- **Health potions** — the 9 hearts (2 through 10).

Shuffle the 44 cards into the face-down **Dungeon**. Starting **health is 20** (hard cap; you
can never heal above 20, except in the one scoring edge case noted below).

### Card values
- Monster damage = ordered value: 2–10 face value, J=11, Q=12, K=13, A=14.
- Weapon value = 2–10 (its number).
- Potion heal = 2–10 (its number).

### Turn structure (the Room)
- A **Room** is 4 face-up cards. Flip from the Dungeon until 4 are showing.
- **Avoiding:** you may scoop all 4 cards and place them at the bottom of the Dungeon.
  You may avoid any number of rooms, but **never two rooms in a row**.
- If you don't avoid, you must resolve **3 of the 4 cards**, one at a time. The remaining
  4th card carries over as the first card of the next Room.

### Resolving a card
- **Weapon:** binding — you must equip it, discarding your previous weapon (and any monsters
  stacked on it).
- **Potion:** add its value to health (capped at 20), then discard. **Only one potion heals
  per turn** — a second potion taken in the same turn is discarded and does nothing.
- **Monster:** fight it barehanded or with the equipped weapon.

### Combat
- **Barehanded:** subtract the monster's full value from your health; discard the monster.
- **With a weapon:** damage taken = max(0, monster value − weapon value). (Weapon 5 vs a 3
  monster → 0 damage; weapon 5 vs a Jack(11) → 6 damage.) Stack the monster on the weapon.
- **Weapon degradation (IMPORTANT):** once a weapon has slain a monster, it can only be used
  on monsters whose value is **strictly less than the last monster it slew**. Example: a
  5-weapon that killed a Queen(12) can still be used on a 6 (6 < 12); but after it is used on
  a 6, it can only be used on monsters of value 5 or lower — another 6, or a Queen, would
  have to be fought barehanded. The weapon is **not** discarded when it can't be used; it
  stays equipped for weaker monsters.

### End and scoring
- The game ends when health reaches 0, or you clear the entire Dungeon.
- **If health reached 0:** sum the values of all monsters still left in the Dungeon and
  subtract that from your (zero/negative) life. The resulting negative number is your score.
- **If you cleared the Dungeon:** your score is your remaining positive life. Special case:
  if your life is 20 *and* the last card resolved was a health potion, your score is
  20 + that potion's value (the only way the total exceeds 20).

## Extensibility (the seams, and what is built on them)

These seams were designed up front for achievements, difficulty/variation modes,
and new cards with special abilities. **Shipped:** high scores and lifetime stats
(`runs`), achievements (`achievements`), and three difficulty modes — Standard,
Relentless, Frail — as `Rulesets` factories named by the `GameModes` catalog.
**Not built:** new cards with special abilities. Keep designing for it; don't
build it speculatively.

- Cards are data-driven. A card references a definition (id, type, value, and an
  effect that applies itself to the game state). Resolving a card dispatches to its
  effect — NOT a switch on suit. Build the standard 44-card deck as a default
  dataset; new cards must be addable as new definitions + effects without changing
  the core turn loop.
- All rules and constants live in an injected Ruleset/GameConfig (starting health,
  room size, cards resolved per turn, health cap, potions per turn, avoid rules,
  scoring strategy, and the deck definition to use). The engine hardcodes nothing.
  Variations and difficulty = different Ruleset instances, not new code.
- The engine stays ignorant of features. apply(move) returns the new state AND the
  events that occurred (e.g. monster slain, potion wasted, weapon broke, room
  avoided, game won). Achievements/stats/UI observe these from OUTSIDE core. The
  core module must never import achievements, persistence, or UI.
- Keep GameState as plain, serializable records. Local persistence exists for runs
  and achievements (tolerant, versioned `key=value` logs under `~/.scoundrel/`,
  with a recoverable soft-delete). There is still no mid-game save/resume and no
  replay format — don't build either.

Guardrail: no speculative abstraction. A new difficulty is a `Rulesets` factory
plus a `GameModes` entry — not new engine code; still the three base card effects,
still no plugin framework. Every extension point must be justified by one of the
named future features; if it isn't, leave it out.
