# Scoundrel

A desktop implementation of **Scoundrel**, a single-player roguelike card game, built in Java with [libGDX](https://libgdx.com/) (LWJGL3 desktop backend).

Scoundrel is a solitaire dungeon crawl played with a trimmed deck of 44 cards. You descend through a "dungeon" of face-up cards, fighting monsters, picking up weapons, and drinking health potions, trying to survive to the bottom of the deck. It's quick, tense, and entirely a game of managing risk with the hand the shuffle deals you.

> **Status:** playable. The full base game works — a pure, fully unit-tested rules engine, a Scene2D UI (framed card tiles, press to resolve, a procedural torchlit backdrop with drifting embers), card motion, per-card resolve effects and a cinematic YOU DIED death screen, persisted high scores, a records screen, achievements, three difficulty modes, and a guided tutorial for new players. Only drawn card art and creature sprites are still to come. See [Roadmap](#roadmap).

## How to play

The deck is 44 cards: the 26 clubs and spades are **monsters**, the diamonds 2–10 are **weapons**, and the hearts 2–10 are **potions**. You start with **20 health** (which is also the cap — you can't heal above it).

- **Rooms.** Cards are dealt four at a time from the dungeon. Each set of four is a *room*.
- **Avoiding.** Instead of fighting, you may scoop up the whole room and put it back at the bottom of the dungeon — but you can't avoid two rooms in a row.
- **Resolving.** If you don't avoid, you resolve three of the four cards one by one; the fourth carries over into the next room.
  - **Weapon:** equip it, discarding whatever weapon (and stacked monsters) you had before.
  - **Potion:** heal by its value, capped at 20. Only the first potion each turn does anything; extras are wasted.
  - **Monster:** fight it bare-handed (take its full value as damage) or with your weapon (take `monster − weapon`, never below 0).
- **Weapon degradation.** Once a weapon kills a monster, it can only be used afterward on monsters **strictly weaker than the last one it slew**. It stays equipped for weaker foes, but equal or tougher monsters must be fought bare-handed.
- **Winning & losing.** You lose when health hits 0, and win by clearing the entire dungeon. A cleared dungeon scores your remaining health; dying scores a negative number based on the monsters left unfought.
- **Difficulty modes.** *Standard* is the game above. *Relentless* forbids avoiding entirely — every room must be faced. *Frail* starts you at 14 health and caps healing there. Achievements are earned in Standard only; high scores are ranked separately per mode.

## Build & run

**Prerequisites:** a JDK matching the project's language level (currently Java 21 — see `java.sourceCompatibility` in `build.gradle`). The Gradle wrapper is included, so you don't need a separate Gradle install.

```sh
# Play the game (desktop launcher)
./gradlew lwjgl3:run

# Run the rules-engine tests
./gradlew core:test
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Project structure

This is a Gradle multi-module project. The intended split (per `CLAUDE.md`) is:

- **`core`** — all game logic. The rules engine (`model` + `rules` packages) is **pure, headless Java with no libGDX dependency**, unit-tested without a window or render loop. The Scene2D screens (`screens` package) draw the state and translate presses into engine moves — no rule logic lives there. `runs` records finished games and derives high scores and lifetime totals; `achievements` evaluates and persists unlocked achievements — both pure Java, observing the engine from outside.
- **`lwjgl3`** — a thin desktop launcher (LWJGL3). No game logic.

See [`CLAUDE.md`](CLAUDE.md) for the full design rules and the complete game specification.

## Roadmap

- [x] Pure model: deck, cards, and game state in `core`
- [x] Rules engine: room dealing, avoiding, combat, weapon degradation, scoring
- [x] Unit tests covering the tricky rules (degradation, one-potion-per-turn, scoring edge cases)
- [x] Scene2D UI to play a full game (plain version: typed tiles, event feed, end overlay)
- [x] Motion: cards deal in and sweep away on avoid; HP damage/heal pulses; per-card resolve effects (bare-handed strike, weapon→battleaxe, potion pour, weapon-kill slice); a YOU DIED death cinematic
- [x] Ambient atmosphere — procedural torchlit backdrop, flicker, drifting embers; framed cards
- [ ] Card art and creature sprites (drawn illustration)
- [x] High scores (persisted to `~/.scoundrel/runs.log`; best shown on the end screen)
- [x] Lifetime stats — THE LEDGER records screen (top runs + totals over the run log)
- [x] Achievements (built on the engine's event stream; earned via a TROPHIES screen)
- [x] Difficulty variants — Standard, Relentless, Frail (alternate rulesets, no engine changes)
- [x] Guided tutorial — a scripted first run that teaches every rule; auto-offered once, replayable from "How to play"

## Credits & acknowledgements

Scoundrel was designed by **Zach Gage and Kurt Bieg**.

This repository is an independent, non-commercial fan implementation built for learning. It is **not affiliated with, endorsed by, or sponsored by** the original designers. All credit for the game's design belongs to them.
