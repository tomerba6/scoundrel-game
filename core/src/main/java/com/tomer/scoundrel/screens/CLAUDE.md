# Screens — rendering rules

These are the rendering half of the root `CLAUDE.md`'s *Sprite art — these are hard rules*,
kept here so they load when a screen is opened rather than in every session. They are just as
binding. Section numbers (§8, §10, §11) refer to the root `HANDOFF.md`.

- **`TextureFilter.Nearest`, integer scales only (1, 2, 3, 4), whole-pixel positions.** These are
  hand-placed pixels; a fractional scale or a sub-pixel offset invents colours outside the
  palette and makes the art shimmer. `Math.round` every computed position before drawing.
- **`PixelViewport(1280, 720)`** on every screen. Every number in `HANDOFF.md` is in that space;
  sprites draw at ×2 = 128px inside a 176×256 card. Do not re-derive the layout per window size.
  It fits like a `FitViewport` (it extends `Viewport` directly) but snaps the scale **down to a
  multiple of 0.5** and letterboxes the rest — a plain fit gives ×1.25 at 1600×900, which puts 2.5 screen pixels on each source pixel
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

The mock (`art-reference/Scoundrel - Sprite Directions.dc.html`) is the visual target: the board
at 1280×720, every effect in isolation, all 26 idle cycles, and all six screens. It opens in a
browser — ask the user to compare against it; you can't render it yourself.
