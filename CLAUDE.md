# Scoundrel — Project Memory

A desktop implementation of **Scoundrel**, a single-player roguelike card game, built with LibGDX.

## Tech stack
- Java (language level 21), Gradle multi-module project.
- LibGDX with the **LWJGL3** desktop backend only. No Android/iOS/web modules.
- UI built with Scene2D (`scene2d.ui`).

## Commands
- Run the game: `./gradlew lwjgl3:run` (Windows: `gradlew.bat lwjgl3:run`).
- Run tests: `./gradlew core:test`. Tests use JUnit 5 and live in `core/src/test/java`.

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
  - `...scoundrel.screens` — Scene2D screens (LibGDX-dependent).
  - `...scoundrel.runs` — run recording + local high-score persistence (pure Java;
    observes the engine from outside — `model`/`rules` never import it).
  - `...scoundrel.achievements` — achievement definitions, evaluation, and the
    unlocked-latch persistence (pure Java; observes the engine + run log from
    outside — `model`/`rules`/`runs` never import it).
- **Detailed design reference:** the full rules-engine design — the `model`/`rules`
  types, the turn loop, extension seams, and the locked edge-case decisions — is
  documented in [`docs/design.md`](docs/design.md) (prose + Mermaid diagrams).
  Consult it when working on the engine, and keep it in sync when the design changes.
- **UI layer reference:** the Scene2D UI — locked interview decisions, theme tokens,
  architecture, and every on-screen component — is documented in
  [`docs/ui.md`](docs/ui.md). The current UI is the deliberate *plain* version —
  flat typed tiles, no card art. Only card art and sprites remain for a later
  pass; card motion (deal-in, avoid-sweep) and HP pulses have already shipped
  (the `Choreographer`). Consult it when working on screens, and keep it in sync
  when the UI changes.

## Working preferences
- For any non-trivial change, propose a plan first and wait for review before coding.
- Write unit tests for the rules engine, especially the tricky rules below. Prefer getting
  tests green before touching the UI.
- Keep commits small and focused; commit after each working piece.

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
