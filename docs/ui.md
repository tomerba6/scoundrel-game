# Scoundrel — UI Layer

This documents the UI: the decisions locked in the design interview, the
visual tokens, the architecture, and every component on screen. It complements
[`design.md`](design.md) (the rules engine); keep both in sync with the code.

> ## ⚠ The board has moved on — read this first
>
> **The game board is no longer Scene2D.** The pixel-art conversion
> ([`HANDOFF.md`](../HANDOFF.md)) replaced it with immediate-mode drawing at a
> fixed 1280×720: `GameScreen` draws onto a batch, `BoardView` owns the room and
> every effect, `BoardHud` the chrome, `CardFace` the printing on a card, and
> `BoardArt`/`CardArt`/`HudArt` hold the measurements. `CardTiles`,
> `Choreographer` and `Motion` are **gone**, and so is the *Ashen* palette —
> the 80-colour ramp system in `HANDOFF.md` §6 supersedes it.
>
> Everything below still describes the board accurately as **behaviour** —
> what a press does, when Avoid is live, what the feed says, why a click is
> never swallowed. Where it describes Scene2D *mechanics* for the board, read
> it as history. The title, mode picker, ledger and trophies screens have since
> been converted and are described as they now are. Only the **run end and
> tutorial overlays** are still Scene2D exactly as written; they are the last
> pass, and this document is rewritten when they land.
>
> **Where the look stands.** The board is a torchlit dungeon: a procedural
> backdrop (warm glow with a live flicker over a vignette, drifting embers) and
> hand-drawn 64×64 pixel sprites at ×2 inside 176×256 framed cards. Motion
> (deal-in, avoid-sweep, HP pulses, the per-card resolve effects, and the YOU
> DIED death cinematic) steps on a 12 fps grid; idles run at 6.

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
     card vanished into the stage. Cards no longer go through Scene2D at all —
     the board hit-tests its own rectangles (`CardHitRegions`) — but the trap
     is still live for the overlays: it made the end overlay non-modal (presses
     fell through to the dead board), so it is explicitly `enabled`. Any
     full-screen or overlapping actor needs a deliberate `Touchable` decision.
  2. **Cards, chooser buttons, and the animation gate act on _press_, not
     click** (`Widgets.pressListener`). Scene2D's `ClickListener` only fires
     when the release lands back on the same actor, so fast play — where the
     mouse is already travelling to the next card as the button comes up —
     silently lost clicks. Real buttons (Avoid, New game, Records) keep
     release semantics, so a press can still be cancelled by sliding off.
- **Menu buttons act on release, and show the press while they wait**
  (`PressGesture`, added 2026-08-10). The pixel menus hit-test their own
  rectangles, so they get none of Scene2D's button behaviour for free and had
  been acting on the press instead. Three parts, and the third is the one that
  is easy to miss:
  1. The release only counts where the press began. Sliding off lifts the plate
     and cancels; sliding back on re-arms it, because a wobble on the way to a
     click is not a change of mind.
  2. A held plate is drawn **pressed** — the bevel inverted, the face on its
     shadowed step, the label travelled 2px down and right into the recess
     (`Chrome.plate`). Release semantics without this read as a button that did
     not work. A mode panel is the button on its own screen, so it takes the
     same treatment; its *frame* stays put, because 1200px of panel shifting
     bodily would eat the 14px gap below it and read as a layout fault. The
     recess is drawn inside the frame, so a selected panel keeps its gold edge
     and gains the press rather than choosing between them.
  3. A screen with more than one kind of target hit-tests into **one id space**,
     because the gesture matches a release against a press by equality and
     cannot know which family an index came from. Panels and buttons are their
     own index, −1 is nothing, and shared chrome takes the negatives below that
     (`ScreenArt.BACK`). The two families must not overlap on screen either, or
     a point would have two answers and which won would come down to the order
     of the ifs — pinned by `ScreenArtTest`.
  4. **The action fires from `render`, not from the release.** A click is often
     shorter than four frames and every menu button navigates, so acting the
     instant the button came up cut to the next screen before the sunk plate had
     been drawn once — release semantics that felt *less* responsive than the
     press-to-act they replaced. The release arms; `PressGesture.advance` fires
     once the plate has actually been on screen for 60ms. Note the hazard that
     goes with it: navigating disposes the calling screen, so `render` must
     `return` immediately after — see the death-cinematic note below.
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
| dried blood | `#8c2f22` | monster tiles, slain chips, the YOU DIED reveal & death bleed |
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

Both faces are on their way out. **Silkscreen** replaces them screen by screen
as the pixel-art conversion lands (`HANDOFF` §5); the board, title, new game,
ledger and trophies are already on it, and the two remaining Scene2D overlays
still hold the old pair. Its sizes are the only ones anything may ask for, and
they live in `PixelType`: 8, 12, 14, 26, 38. All even, because the viewport
snaps to half-steps and an odd size lands on a half pixel at ×1.5.

**Nothing below 12 for anything a player reads.** Silkscreen's strokes are 1px,
so at the ×1.5 a 1920×1080 display gets, each stroke lands on either one screen
pixel or two depending where it falls. On a 12px glyph that is coarse; on an 8px
one it is the difference between a letter and a smudge. `SMALL` (8) survives for
incidental furniture only — a portrait caption, a credit line. When a string
does not fit at 12, wrap it and grow the row (`TextWrap`) rather than dropping
to 8.

## Architecture

- **View = f(state), drawn from scratch each frame.** `GameScreen` holds the
  immutable `GameState`; every move runs `apply` and replaces it, and the board
  is drawn from that state on every frame. There is nothing to update
  incrementally — if the state is right, the screen is right. Motion never
  weakens this: an effect plays *over* a board that is already final, so no
  animation ever carries game state.
- **`BoardView`: the room and everything that happens to it.** It owns the idle
  clock, each card's stagger, and the single effect currently running; it owns
  no game state and no rules. `GameScreen` tells it which cards are in the room,
  what just happened to one of them, and where the pointer is. The developer lab
  (`SpriteLab`, F9) draws through the same class, which is the only thing that
  makes it a useful instrument.
- **Effects are blocking but skippable, and no click is ever wasted.** While one
  plays, a press settles it *and* resolves whatever card it landed on. Always
  safe, because the state underneath is already final. Each effect is a pure,
  unit-tested timeline quantised to a 12 fps grid — `CardFlight` (the deal, the
  avoid sweep, the equip carry), `Barehanded`, `WeaponKill`, `PotionDrink`,
  `PotionSpill`, `HpPulse`, `DeathCinematic` — and nothing tweens: every value
  holds for a whole frame.

  Resolving a card sets **three clocks** going, and they are not one thing:
  what happens to the card that left, the survivors re-centring around the gap,
  and whatever the dungeon sends up to replace it. The first two run *together* —
  the room closes over the gap as the monster dies — while the deal waits, because
  a fresh card flying over the one being killed is a different thing entirely.
  `RoomMotion` is the four-case decision, and it has its own test because "which
  clock is this card on" was got wrong twice.

  A fresh card comes out of the depth ticker, the dungeon made physical, and the
  ticker keeps counting it until it lands. The heal from a potion waits for the
  bottle to actually pour, so the bar filling always has a visible cause — and a
  *wasted* potion never goes near the bar at all, spilling grey where it stood.
- **Death cinematic.** A losing blow withholds the end overlay until the
  cinematic has run, and at ~5.5s it is deliberately the slowest thing in the
  game — every other effect was cut to get here, and this is what the time was
  taken for. The fatal blow plays at **half speed**; the board is then left
  **standing, lit and unmoving** for the best part of a second, long enough to
  read the empty bar and the number under zero. Only then does the torch gutter
  out over it — held values, not a ramp, because a dying flame flares back —
  while the screen goes out by **ordered 4×4 dither: pattern, never alpha**, so
  the board thins out rather than dimming.

  The **depth ticker is drawn over the dither** and outlives everything else, so
  the last thing on screen is the gauge that says how far you got; then it goes
  too, a beat before YOU DIED grows in. The dungeon deals nothing after a fatal
  blow — the room closes over the gap, but you are not being sent another card.
  A click fast-forwards straight to the settled screen at any point. Only losses
  animate — a win keeps the instant `DUNGEON CLEARED` overlay, and the tutorial
  never dies.
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
- **`PixelViewport` at a fixed 1280×720 virtual resolution** — all layout math
  in one coordinate system, any window size letterboxes. Fonts are generated
  once at design sizes. It is a `FitViewport` that snaps the scale down to a
  multiple of 0.5 rather than fitting exactly, so a 64×64 sprite drawn at ×2
  always lands on a whole number of screen pixels; a plain fit gives ×1.25 at
  1600×900 and the pixel art crawls. The scale arithmetic is the pure,
  headlessly tested `PixelScale`, leaving the viewport itself a thin shell.
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

- **Top strip** — the banded health bar with its reading beside it (`14 /20 HP`,
  unclamped: a killing blow shows how far past zero it took you, and only the
  bar's fill clamps); the **depth ticker** (one tick per card of the deck, gold =
  still face-down, dark = gone) with a `DEPTH n · m:ss` caption under it carrying
  a live run timer — `RunRecorder.elapsedSeconds` off the recorder's
  clock, ticked each `render`, frozen at the final time on game over (so it agrees
  with the recorded `RunRecord.seconds`), and formatted by the shared `ClockText`.
  The tutorial has no recorder, so it shows no timer. The **Avoid** button
  (torchlight when legal, stone when not).
- **Backdrop** — behind every screen, drawn first and never a hit target
  (`Backdrop`): a procedural torch glow (a generated radial texture, tinted
  torchlight, its alpha modulated by `TorchFlicker` — a pure, tested sine-blend
  so it breathes without looping) over a generated vignette, plus drifting
  embers (`Embers`, a pure tested particle sim). All from `Theme`'s generated
  textures; no external assets.
- **Room row** — up to four 176×256 cards, centred for however many are in the
  room. Each is an outer bezel, a role-coloured plate with 2px light/dark
  bevels, and a recessed well holding the 64×64 sprite at ×2. A 26px header
  carries the rank, its suit pip and the type; the footer carries the value at
  38px with a hard 4px drop shadow. Plates come from the ramp system
  (`CardArt.paletteFor`): oxblood monster, slate weapon, moss potion.
- **Chooser** — a stack of the board's own gold plates over the pressed card,
  one per legal move (`USE WEAPON` / `BAREHANDED`), drawn by the same
  `BoardHud.drawPlate` the Avoid button is. Generic: a future card offering three
  moves gets three plates. Every plate takes the widest label's width — a ragged
  stack reads as two unrelated buttons rather than one choice. Geometry and hit
  test are `ChooserArt`; a press *outside* it dismisses the chooser **and**
  resolves the card it landed on, so the press is never spent merely closing it.
- **Trophy rail** (bottom-left) — the equipped weapon's own sprite in a recessed
  well, its name and value beside it, then slain-monster chips in kill order, and
  the threshold plate: `SLAYS ANYTHING` (fresh), `SLAYS < N`, or `SPENT` (slew a
  2). Reads `BAREHANDED` when nothing is equipped — the well is always there, so
  equipping does not shift the strip sideways.

  The rail **lags the engine deliberately**. A move settles the instant you press
  the card, but a weapon still hopping down to the well has not arrived and a
  monster still being cleaved has not died, so neither the icon nor the chip
  beside it may appear yet: `BoardView.railAhead` holds the previous state until
  what you can see agrees.
- **Potion marker** (bottom-right) — the potion sprite in its well with
  `POTION READY` or `POTION USED` beside it. Never dimmed: an alpha over the dark
  board would make colours that are on no ramp, so the label carries the state.
- **Fading feed** (top-right) — up to four lines, fading after ~4s:
  "Slew the Queen of clubs — took 6", "Fought … barehanded — took 12",
  "Drank the 7 of hearts — healed 5" / "— already full",
  "… wasted — one potion a turn", "Equipped the 5 of diamonds",
  "The weapon dulls — slays < 6" / "The weapon is spent",
  "Avoided the room".
- **End overlay** — dim soot over the board. A **win** reads `DUNGEON CLEARED`
  (torchlight); a **loss** instead runs the death cinematic (the YOU DIED reveal,
  above) and settles this same panel in beneath it. Either way: the score in
  display type, a dim **breakdown line** naming where that number came from
  (`Labels.scoreBreakdown` — `-9 health, minus 175 still in the dungeon` on a
  loss, since a death score charges you for monsters you never saw; the health
  you kept, or cap-plus-final-potion, on a win), the run's final `time m:ss`,
  a best-score line
  (`New best!` in torchlight, or `best N` dimmed — from the persisted run
  history), any achievements just unlocked under a torchlight
  `ACHIEVEMENT(S) UNLOCKED` heading (a hidden one is revealed the moment it is
  earned), then **New game** (reshuffles in place, in the same mode),
  **Trophies**, and **Records**.
- **Title screen** — `SCOUNDREL` in display type over soot, then New game,
  **How to play**, Records, and Trophies buttons, and a dim designer-credit
  line. **New game** opens the mode picker; **How to play** starts the tutorial.
  On the very first launch (`!TutorialFlag.isSeen()`) a modal **New here?** prompt
  pops over the menu — **Play tutorial** / **Maybe later** — and either choice
  marks it seen, so it is offered exactly once.
- **Tutorial** — a scripted, guided run played on the real board
  (`GameScreen` in tutorial mode, given a `TutorialGuide`). It deals the curated
  `TutorialScript` deck, records nothing, and layers guidance on top: each step
  marks its target — a room card or the Avoid button — with a pulsing **bone
  frame outline** (bone, not torchlight, so it reads against both the dark cards
  and the lit Avoid button), and a **contextual callout** of the narration that
  points above the card, below the Avoid button, or sits centred for
  rule-explanation beats. Input is **gated** to the current step: only the
  outlined card responds (no chooser), and Avoid is live only on the step that
  teaches it. Explanation beats carry a **Next**; a persistent **Skip tutorial**
  corner button leaves to the title. Clearing the deck shows a **Tutorial
  complete** screen that prints the run's actual score and reads it back as the
  rule that produced it (`Labels.tutorialScore`), then Play for real / Main menu.
  The whole thing teaches every rule in sequence — combat, degradation both ways,
  potions and the one-per-turn cap, avoiding and never-twice, and **scoring** in
  two centred beats where it bites: the negative losing score right after the
  bare-handed 9 halves your health, and what a cleared dungeon is worth (plus the
  20-plus-potion flourish) with the last monster still standing. It is proven
  end-to-end by a headless test that plays the script through the engine to a
  win, and that pins the winning beat's promise — `score == health left` — against
  the real scoring strategy.
- **NEW GAME (mode picker)** — headed `NEW GAME` over `Choose your descent.`,
  one hairline-ruled row per mode from `GameModes.all()`: the mode's name as the
  torchlight button that starts the run, what it changes, and a right-aligned
  `trophies count` / `no trophies` tag. That tag states the Standard-only
  achievement rule *where the choice is made*, so a variant run never silently
  fails to unlock. Back returns to the title. A new mode appears here with no
  change to the screen.
- **THE LEDGER (records screen)** — the top 10 runs in one framed table, rows
  striping `191513`/`141110` by **flat colour, never alpha**, beside a 296px
  totals panel. Seven columns: Roman-numeral rank, score (cream, dried blood
  when negative), outcome (`71b45c` cleared / `8c2f22` defeated), **the mode it
  was run in**, date, duration, monsters slain. The mode tag is required
  reading, not decoration: scores are ranked per mode, so without it a Frail 14
  beside a Standard 20 is unreadable. An id with no matching mode renders as the
  raw id rather than taking the screen down. The table's height follows its row
  count, so a short log shrinks the frame instead of leaving it hanging. Empty
  state invites a first run. What a row *says* is the pure `LedgerRow`.
  - The totals panel is **not** the reference render's list. That render asks
    for `WEAPONS BROKEN`, and weapons never break in Scoundrel — they degrade
    and stay equipped, which is the rule the whole game turns on. The row was
    drawn from placeholder data, so the panel keeps the render's shape and
    eight-row rhythm and is filled with figures the game actually keeps
    (`LedgerTotals`), two of them — the score extremes and the fastest clear —
    derived rather than stored.
- **Erase-progress confirmation** — a destructive reset is never one press. The
  Erase control (ledger only — deliberately the one such button in the whole
  app) opens a modal that names exactly what will be lost (`N recorded runs and
  M trophies`). The ledger goes under the **modal dim** first, since the dialog
  asks about the very thing behind it. **Keep it** is the gold plate — the
  opposite of how the rest of the game uses gold, and deliberate: the prominent
  choice here is the safe one. Both plates take release semantics and the
  pressed state, so even `ERASE EVERYTHING` can be taken back by sliding off,
  which is verified by screenshot rather than assumed. Escape backs out of the
  dialog before it backs out of the screen. Confirming wipes both logs as a
  *soft* delete: `clear()` moves each file to a `.bak` sibling rather than
  deleting, so a mistake is recoverable from disk (the game never auto-restores
  it). The ledger then re-shows empty.
- **TROPHIES (achievements screen)** — the whole catalog as a book of deeds, ten
  entries in two columns of five, filled **down then across** (getting that
  row-major would silently reorder the catalog and look entirely plausible).
  Each row: a 26px seal well, the title, the deed, and the date won. The seal's
  fill is the *only* difference between earned and locked — §11 rules out a
  padlock glyph and a greyscale filter, so an empty well is the locked state. A
  hidden trophy stays `???` until earned, then reveals its real title and text.
  The header carries a 160×16 progress bar built exactly like the board's health
  bar, down to reusing its dark-green empty track under a gold fill — that reads
  like an oversight and is not, it is what the reference shows. The date a
  trophy was won is kept, which the render drops; it is real information and the
  row has the width. What a row says is the pure `TrophyEntry`.

## Files

- `core/src/main/java/com/tomer/scoundrel/screens/Theme.java` — tokens
  (palette, motion timings, card size), fonts, drawables, suit shapes, and the
  4×4 dither tile every modal overlay dims through. The tile lives here rather
  than on `Chrome` because `Theme` is the one shared, disposed object; a texture
  on `Chrome` would leak once per screen.
- `core/src/main/java/com/tomer/scoundrel/screens/Chrome.java` /
  `ScreenArt.java` — the menu kit: the five parts every screen outside the board
  is assembled from (frame, face, bevel, label, rule), plus `plate` — the one
  button shape, at every size, raised or held down — `header` for the band four
  screens share, and `dim` for a modal. All the arithmetic is in `ScreenArt`
  where it is tested; `Chrome` only turns measurements into draw calls.
- `core/src/main/java/com/tomer/scoundrel/screens/GameScreen.java` — the one
  screen: layout builders, interaction, feed, overlay, run recording, and
  (given a `TutorialGuide`) the guided-tutorial mode — glow outline, callout,
  gating, and the Tutorial-complete ending.
- `core/src/main/java/com/tomer/scoundrel/tutorial/` — the pure tutorial logic:
  `TutorialScript` (curated deck + narrated steps), `TutorialStep`,
  `TutorialGuide` (the gating state machine), and `TutorialFlag` (the seen
  marker). No LibGDX; drives the engine via its ordered-deck entry.
- `core/src/main/java/com/tomer/scoundrel/screens/BoardView.java` — the room
  and every effect that plays over it, drawn in immediate mode. Shared with the
  developer lab so the instrument shows what ships. (Replaces `Choreographer`.)
- `core/src/main/java/com/tomer/scoundrel/screens/BoardHud.java` /
  `CardFrame.java` / `CardFace.java` / `Pips.java` / `EffectArt.java` — the
  board's chrome, the card frame, the printing on a card, the four suit pips,
  and the generated shapes the effects use. `BoardHud.drawPlate` is the board's
  one button shape: the Avoid button and every choice in the move chooser are
  the same method, so they cannot drift apart.
- `core/src/main/java/com/tomer/scoundrel/screens/PixelSurface.java` — the
  offscreen target, exactly the design size, that the board is drawn onto so the
  finished image is scaled to the window **once**. Without it every draw call is
  scaled and rounded on its own, and at a fractional window scale identical
  features disagree — see HANDOFF §4.
- `core/src/main/java/com/tomer/scoundrel/screens/TitleScreen.java` /
  `ModeSelectScreen.java` / `RecordsScreen.java` / `TrophiesScreen.java` — the
  navigation anchor, the difficulty picker, THE LEDGER, and the achievement
  catalog. All four are on the pixel kit and draw in immediate mode; only the
  board's overlays are still Scene2D.
- `core/src/main/java/com/tomer/scoundrel/screens/Widgets.java` — shared label
  and button builders for the screens that are still Scene2D, plus
  `pressListener` (the press-not-click input rule).
- `core/src/main/java/com/tomer/scoundrel/screens/Backdrop.java` — the ambient
  layer added first to each stage: torch glow, vignette, and embers.
- `core/src/main/java/com/tomer/scoundrel/screens/CardHitRegions.java` /
  `TorchFlicker.java` / `Embers.java` / `ClockText.java` / `FeedText.java` /
  `Feed.java` / `Labels.java` / `ResolveEffect.java` / `BoardArt.java` /
  `CardArt.java` / `HudArt.java` / `PixelScale.java` / `PixelType.java` /
  `PressGesture.java` / `LedgerRow.java` / `LedgerTotals.java` /
  `TrophyEntry.java` / `TextWrap.java` — the
  pure, headlessly unit-tested logic behind the screens: the "which card is
  under this point" lookup, the flicker curve, the ember sim, the
  run-timer/duration formatting, the event-feed text and its stepped fade, the
  labels, the animation-routing decision, and every board measurement.
- `core/src/main/java/com/tomer/scoundrel/screens/ChooserArt.java` /
  `RoomMotion.java` / `HealthReadout.java` / `PipMask.java` — the same idea for
  the decisions the board makes rather than the numbers it holds: where the move
  chooser's plates go, which clock each card of the room is on, what the health
  bar says (which is not always what the state says), and the four suit pip
  shapes. Each of these was a branch inside a screen before it was a class, and
  three of the four were extracted because that branch had a bug in it.
- `core/src/main/java/com/tomer/scoundrel/screens/CardFlight.java` /
  `WeaponKill.java` / `Barehanded.java` / `PotionDrink.java` / `PotionSpill.java` /
  `HpPulse.java` / `DeathCinematic.java` / `IdleCycle.java` — one pure timeline
  per effect, each quantised to the 12 fps grid (6 for idles) and unit tested.
  `PotionSpill` is the wasted potion, which goes nowhere near the health bar.
- `core/src/main/java/com/tomer/scoundrel/screens/SpentMask.java` /
  `HurtMask.java` / `RimMask.java` — pixel transforms applied at load rather than
  tints applied at draw, so no blend can land a colour between two ramp steps:
  the drained bottle, the struck creature, and its outline.
- `core/src/main/java/com/tomer/scoundrel/ScoundrelGame.java` — the navigator:
  creates the Theme, RunLog and AchievementStore, boots into `TitleScreen`,
  owns disposal.
- `lwjgl3` launcher — 1280×720 window, title "Scoundrel".
- `assets/fonts/` — the two typefaces plus OFL license texts.

## What the illustration pass will change (and what it won't)

The atmosphere and card framing have shipped in `Theme` and `Backdrop`. The one
remaining visual pass is *illustration* — drawn card art and creature sprites
inside the existing frames — which again lands mostly in `Theme` (swap the flat
panels and drawn suit pips for art) and touches no screen logic. What should
*not* change: the dumb-view rule, the state-rebuild model as the source of
truth, the legalMoves-driven interaction, and the event-stream feed. If an
animation needs to know a rule, that's a sign the engine should expose it, not
the UI re-derive it.
