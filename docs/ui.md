# Scoundrel — UI Layer (plain version)

This documents the Scene2D UI as it exists today: the decisions locked in the
design interview, the visual tokens, the architecture, and every component on
screen. It complements [`design.md`](design.md) (the rules engine); keep both
in sync with the code.

> **This is not the final look.** The current UI is the deliberate *plain*
> version: flat color tiles, drawn suit shapes, text everywhere. Card art,
> sprites, and ambient atmosphere are a later pass — the layout, theme seams,
> and interaction model below were built so that pass swaps assets without
> rewriting screen logic. Motion already ships (see `Choreographer`).

## Locked decisions (from the design interview)

- **Layout: room-centered.** The four room cards dominate the center; thin HUD
  strips top and bottom. No sidebar.
- **Cards: typed tiles.** Role-colored (monster/weapon/potion), big value,
  small rank + suit index in the corner.
- **Input: press-then-pick.** A card with one legal move plays immediately; a
  monster with both fight options pops a small two-button chooser at the card.
  Avoid is a HUD button.
- **A press must never be swallowed.** This UI's recurring bug. Two Scene2D
  traps caused it, both fixed and both worth remembering:
  1. **`Table` defaults to `Touchable.childrenOnly`** — the table itself is
     never a hit target, only its children are. A card tile is a Table, so
     only its *label glyphs* were clickable and presses on the blank part of a
     card vanished into the stage. `CardTiles.makeWholeFaceHittable` sets
     `Touchable.enabled`; `CardTileHitAreaTest` pins it. The same trap made
     the end overlay non-modal (presses fell through to the dead board), so it
     is explicitly `enabled` too. Any full-screen or overlapping actor needs a
     deliberate `Touchable` decision.
  2. **Cards, chooser buttons, and the animation gate act on _press_, not
     click** (`Widgets.pressListener`). Scene2D's `ClickListener` only fires
     when the release lands back on the same actor, so fast play — where the
     mouse is already travelling to the next card as the button comes up —
     silently lost clicks. Real buttons (Avoid, New game, Records) keep
     release semantics, so a press can still be cancelled by sliding off.
- **Event log: fading feed.** The last few events float top-right and fade;
  no permanent log panel.
- **Flow (revised 2026-07-07):** launch lands on a tiny **title screen** —
  the navigation anchor every future menu (variants) hangs off: New game /
  Records / Trophies / a dim credit line. Win/loss dims the board under an
  overlay with the score, best line, any freshly unlocked achievements, then
  New game / Trophies / Records. **Records and Trophies are reachable only
  between games** (title + end overlay): a run, once started, is
  uninterruptible — consistent with quit-outs being unrecorded.
- **Window:** resizable, 1280×720 default, Fit viewport (letterboxed scaling).
- **Mood: torchlit dungeon** — dark, warm, quiet.

## Design tokens

Palette (all constants in `screens.Theme`):

| Token | Hex | Used for |
|---|---|---|
| soot | `#17130f` | background |
| stone | `#241d16` | frames, strips, popups |
| dried blood | `#8c2f22` | monster tiles, slain chips, DEFEATED |
| iron | `#7a8794` | weapon tiles |
| herbal | `#5d8a4a` | potion tiles |
| torchlight | `#d9a441` | accent: Avoid, threshold plate, ticker, CLEARED |
| bone | `#e8ddc7` | text, health |

Type (generated at startup via `gdx-freetype` from TTFs in `assets/fonts/`,
both SIL OFL, licenses bundled):

- **IM Fell English** — display (64px card values, 42px overlay titles). Its
  old-style numerals are a deliberate period touch (a big "11" reads a little
  like Roman "II"; the corner index always shows the true identity in the sans
  face).
- **Alegreya Sans** regular/bold — HUD labels, buttons, feed, corner indices
  (18px and 14px).

Neither face has suit glyphs, so ♠♥♦♣ are drawn as pixmap shapes in `Theme`
and tinted at use; feed copy writes names out ("the Queen of clubs").

## Architecture

- **View = f(state), rebuilt wholesale.** `GameScreen` holds the immutable
  `GameState`; every move runs `apply`, replaces the state, and rebuilds the
  whole widget tree from it. No incremental widget updates — if the state is
  right, the screen is right. Motion never weakens this: the `Choreographer`
  (below) plays cosmetic flights *over* rebuilds, so widget identity across
  moves is never needed.
- **Choreographer: cosmetic motion over the final board.** After a move the
  board is rebuilt first (truth before motion), then flight proxies — built by
  the same `CardTiles` factory as the real tiles, which hide meanwhile —
  replay the transition above it: dealt cards fly out of the depth ticker
  (the dungeon made physical), the carryover card slides from its old slot,
  and an avoided room sweeps up into the ticker before the next deal.
  Animations are **blocking but skippable, and no click is ever wasted**: a
  fullscreen gate holds input while one plays, and a press on it settles the
  board *and* resolves the card it landed on (`Choreographer.SkipListener` →
  `GameScreen.resolveCardAt`). Always safe, because nothing mid-flight carries
  game state. Durations, stagger, and card size are `Theme` tokens, kept short
  on purpose — the gate covers the whole deal, so a long animation reads as
  dropped clicks (`Motion.dealWindow` pins that span: 0.30s deal, 0.50s after
  an avoid). The pure parts — the window arithmetic and the "which card is
  under this point" lookup — live in `Motion` and `CardHitRegions` so they are
  unit tested headlessly. Locked motion set, both shipped:
  traveling cards (deal-in + avoid sweep) and feedback pulses (damage
  shudders the HP bar and flashes the number dried blood; healing glows the
  fill back in, herbal). No reveals or ambient effects yet.
- **Navigation.** `ScoundrelGame` is the navigator: it owns the shared
  `Theme`, `RunLog`, and `AchievementStore`, exposes
  `showTitle`/`showGame`/`showRecords`/`showTrophies`, and disposes the
  outgoing screen on every switch. Screens are cheap and built fresh each
  time; nothing is cached across switches.
- **Dumb view.** The screen calls only `newGame` / `legalMoves` / `apply`.
  Everything conditional (Avoid enabled, instant-play vs chooser, chooser
  contents) derives from `legalMoves`. Zero rule logic in screens — even
  damage previews were omitted rather than duplicate combat math in the UI.
- **`Theme` owns every visual fact**: palette, fonts, flat drawables (a 1×1
  white texture tinted per use), suit icons. Created once in `ScoundrelGame`,
  disposed once, passed to screens. The sprite pass swaps Theme internals.
- **Programmatic styles, no uiskin.** With zero art, the Scene2D `Skin`
  JSON/atlas adds indirection; styles are built in code, compiler-checked.
  (The unused liftoff `assets/ui/` skin remains and should be removed or
  replaced in the art pass.) An atlas-backed Skin becomes worthwhile when
  real textures arrive.
- **`FitViewport` at a fixed 1280×720 virtual resolution** — all layout math
  in one coordinate system, any window size letterboxes. Fonts are generated
  once at design sizes.
- **Three stage layers with distinct lifetimes:** the root board table
  (cleared per rebuild), the feed anchor (persistent, `Touchable.disabled` so
  it never steals clicks), and transient overlays (chooser, end screen) on
  top. The end overlay is modal because it is fill-parent **and explicitly
  `Touchable.enabled`** — a background alone does not block input, since a
  Table is `childrenOnly` by default.
- **Events feed the feed; state feeds everything else.** `MoveResult.events`
  are consumed once for feed lines; persistent widgets render from state.
  `RoomDealt` and `GameWon/Lost` are filtered (board and overlay own those
  facts). The feed shares the observer seam with the `runs` and `achievements`
  layers — one event stream, consumed for different ends.
- **Run recording and achievements.** `ScoundrelGame` builds a `RunLog`
  (`~/.scoundrel/runs.log`) and an `AchievementStore`
  (`~/.scoundrel/achievements.log`) and hands both to the screen. Each game
  gets a `RunRecorder` and an `AchievementTracker`, both fed every
  `MoveResult`. When the game ends, `finishRun` appends the run, then evaluates
  the achievements newly earned (from the run summary plus history) and appends
  each to the store; those fresh unlocks are listed on the end overlay. The two
  storage steps are independently guarded — a failure in either is logged and
  never interrupts play, nor stops the other.

## Components on screen

- **Top strip** — `HP` label, the charring health bar (bone fill lerping to
  dried blood as health drops), health number; the **depth ticker** (one tick
  per card of the deck, torchlight = still face-down, dark = gone; avoided
  rooms visibly return ticks) with a `depth: N cards` caption; the **Avoid**
  button (torchlight when legal, stone when not).
- **Room row** — up to four typed tiles: type label, display-font value,
  rank + suit index bottom-right. Weapon tiles use soot text on iron for
  contrast; monster/potion tiles use bone.
- **Chooser** — stone popup over the pressed card with one torchlight button
  per legal move ("Use weapon" / "Barehanded"). Generic: a future card
  offering three moves gets three buttons. It carries no padding, so its whole
  area is button; a press *outside* it dismisses the chooser **and** resolves
  the card it landed on, so the press is never spent merely closing the popup.
- **Trophy rail** (bottom-left) — equipped weapon mini-tile, slain-monster
  chips in kill order, and the threshold plate: `slays anything` (fresh),
  `slays < N`, or `spent` (slew a 2). Reads `Barehanded` when nothing is
  equipped.
- **Potion marker** (bottom-right) — `potion ready` (dim) or
  `• potion used this turn` (torchlight).
- **Fading feed** (top-right) — up to four lines, fading after ~4s:
  "Slew the Queen of clubs — took 6", "Fought … barehanded — took 12",
  "Drank the 7 of hearts — healed 5" / "— already full",
  "… wasted — one potion a turn", "Equipped the 5 of diamonds",
  "The weapon dulls — slays < 6" / "The weapon is spent",
  "Avoided the room".
- **End overlay** — dim soot over the board; `DUNGEON CLEARED` (torchlight)
  or `DEFEATED` (dried blood), the score in display type, a best-score line
  (`New best!` in torchlight, or `best N` dimmed — from the persisted run
  history), any achievements just unlocked under a torchlight
  `ACHIEVEMENT(S) UNLOCKED` heading (a hidden one is revealed the moment it is
  earned), then **New game** (reshuffles in place, in the same mode),
  **Trophies**, and **Records**.
- **Title screen** — `SCOUNDREL` in display type over soot, New game,
  Records, and Trophies buttons, and a dim designer-credit line. Deliberately
  empty otherwise; future menus join the button column. **New game** opens the
  mode picker rather than starting a run directly.
- **NEW GAME (mode picker)** — headed `NEW GAME` over `Choose your descent.`,
  one hairline-ruled row per mode from `GameModes.all()`: the mode's name as the
  torchlight button that starts the run, what it changes, and a right-aligned
  `trophies count` / `no trophies` tag. That tag states the Standard-only
  achievement rule *where the choice is made*, so a variant run never silently
  fails to unlock. Back returns to the title. A new mode appears here with no
  change to the screen.
- **THE LEDGER (records screen)** — the top 10 runs as a dungeon ledger:
  Roman-numeral ranks in torchlight, scores in IM Fell (dried blood when
  negative), outcome, **the mode it was run in**, date, duration, monsters
  slain, hairline rules between rows. The mode tag is required reading, not
  decoration: scores are ranked per mode, so without it a Frail 14 beside a
  Standard 20 is unreadable. An id with no matching mode renders as the raw id. Beside it, lifetime totals headed `ACROSS N FINISHED RUNS` (the
  label encodes the quit-runs decision: finished games are the whole
  universe). Empty state invites a first run. Back returns to the title; a
  quiet **Erase all progress** sits opposite it, bottom-right (disabled when
  there is nothing to lose).
- **Erase-progress confirmation** — a destructive reset is never one click. The
  Erase control (records screen only — deliberately the one such button in the
  whole app) opens a modal that names exactly what will be lost (`all N recorded
  runs and M trophies`), with **Keep it** — the prominent torchlight default —
  and a dried-blood **Erase everything**; both keep release semantics, so a
  stray press can slide off and cancel. Confirming wipes both logs as a *soft*
  delete: `RunLog`/`AchievementStore` `clear()` moves each file to a `.bak`
  sibling rather than deleting, so a mistake is recoverable from disk (the game
  never auto-restores it). The ledger then re-shows empty.
- **TROPHIES (achievements screen)** — the whole catalog as a book of deeds,
  headed by an `N of 10 earned` count. One hairline-ruled row per achievement:
  title (torchlight when earned, dim when locked), the deed described, and the
  date won or `locked`. Locked-but-visible trophies show what to aim for; a
  hidden trophy stays `???` until earned, then reveals its real title and text.
  Read once from the `AchievementStore` on entry, guarded; Back returns to the
  title.

## Files

- `core/src/main/java/com/tomer/scoundrel/screens/Theme.java` — tokens
  (palette, motion timings, card size), fonts, drawables, suit shapes.
- `core/src/main/java/com/tomer/scoundrel/screens/GameScreen.java` — the one
  screen: layout builders, interaction, feed, overlay, run recording.
- `core/src/main/java/com/tomer/scoundrel/screens/Choreographer.java` — the
  flight layer: deal-in and avoid-sweep choreographies, input gate, skip.
- `core/src/main/java/com/tomer/scoundrel/screens/TitleScreen.java` /
  `ModeSelectScreen.java` / `RecordsScreen.java` / `TrophiesScreen.java` — the
  navigation anchor, the difficulty picker, THE LEDGER, and the achievement
  catalog.
- `core/src/main/java/com/tomer/scoundrel/screens/CardTiles.java` /
  `Widgets.java` — shared tile, label and button builders, plus
  `pressListener` (the press-not-click input rule) and
  `makeWholeFaceHittable` (the `Touchable` rule).
- `core/src/main/java/com/tomer/scoundrel/screens/Motion.java` /
  `CardHitRegions.java` — the pure, headlessly-tested parts of the motion and
  skip-and-act logic.
- `core/src/main/java/com/tomer/scoundrel/ScoundrelGame.java` — the navigator:
  creates the Theme, RunLog and AchievementStore, boots into `TitleScreen`,
  owns disposal.
- `lwjgl3` launcher — 1280×720 window, title "Scoundrel".
- `assets/fonts/` — the two typefaces plus OFL license texts.

## What the art pass will change (and what it won't)

Later work — card art and sprites inside the existing tile frames and
ambient atmosphere — lands mostly in `Theme` and the existing
`Choreographer`. What should *not* change: the
dumb-view rule, the state-rebuild model as the source of truth, the
legalMoves-driven interaction, and the event-stream feed. If an animation
needs to know a rule, that's a sign the engine should expose it, not the UI
re-derive it.
