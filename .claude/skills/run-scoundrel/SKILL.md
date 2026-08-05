---
name: run-scoundrel
description: Build, launch, and drive the Scoundrel libGDX desktop game on Windows - start the app, click through menus, play cards, and capture screenshots to verify UI changes. Use when asked to run, start, launch, screenshot, play, or manually test Scoundrel.
---

# Running Scoundrel

Scoundrel is a Java 21 / libGDX (LWJGL3) **desktop** game. `gradlew lwjgl3:run`
opens a native 1280x720 OS window titled `Scoundrel`. There is no DOM and no
built-in remote control, so the only way to verify UI work is to **screenshot
pixels and synthesise mouse clicks** through Win32.

That harness is committed here:

```
.claude/skills/run-scoundrel/drive.ps1
```

All paths below are relative to the repo root. Gradle commands are run from
**Git Bash**; the driver is run from **PowerShell**.

## Prerequisites

A JDK 21+ on `PATH` and a real interactive Windows desktop session. Nothing to
install - the Gradle wrapper is committed. There is no headless path: the driver
screenshots the actual screen, so the window must be visible and not minimised.

## Build and test

```bash
./gradlew core:test          # 183 tests, the pure rules engine + observers
./gradlew lwjgl3:compileJava # launcher compiles
```

`core:test` prints almost nothing on success - trust the exit code. To confirm a
specific class ran, read its JUnit XML:

```bash
grep -oE 'tests="[0-9]+" skipped="[0-9]+" failures="[0-9]+" errors="[0-9]+"' \
  core/build/test-results/test/TEST-com.tomer.scoundrel.rules.GameModesTest.xml
```

## Run (agent path) - START HERE

### 1. Launch, backgrounded

`lwjgl3:run` never returns, so it must be backgrounded:

```bash
nohup ./gradlew lwjgl3:run > /tmp/scoundrel-run.log 2>&1 &
```

### 2. Wait for the window, then screenshot

Cold start is **60-120s** (Gradle + JVM + GL init). `-WaitSeconds` polls for the
window, so this single call covers the wait:

```powershell
& powershell -NoProfile -ExecutionPolicy Bypass -File .claude\skills\run-scoundrel\drive.ps1 `
  -WaitSeconds 240 -Actions "shot:C:\temp\scoundrel\title.png"
```

Then **look at the PNG** with the Read tool. A blank frame means it never came up.

### 3. Click and screenshot

Actions are comma-separated and run in order: `click:<x>:<y>`, `key:<name>`,
`wait:<ms>`, `shot:<path>`.

`key:` takes `ESC`, `ENTER`, `SPACE` or `F1`-`F12` — the bindings the game polls
per frame rather than through Scene2D: **F11** toggles fullscreen, **F9** opens
the developer sprite inspector (`SpriteLab`), **Esc** leaves it.

```powershell
& powershell -NoProfile -ExecutionPolicy Bypass -File .claude\skills\run-scoundrel\drive.ps1 `
  -Actions "click:640:346,wait:1200,shot:C:\temp\scoundrel\picker.png"
```

### 4. Stop

```powershell
& powershell -NoProfile -ExecutionPolicy Bypass -File .claude\skills\run-scoundrel\drive.ps1 -Kill
```

## Verified coordinates

Client pixels, origin **top-left**, window 1280x720.

| Screen | Target | x, y |
|---|---|---|
| Title | New game / Records / Trophies | 640,346 / 640,390 / 640,434 |
| Mode picker | Standard / Relentless / Frail | 176,153 / 176,204 / 176,255 |
| Mode picker | Back | 92,680 |
| Game board | Avoid button | 1214,48 |
| Game board | card centres, y = 363 | see formula below |

The room row is **centred**, so card centres shift as the room shrinks. Cards are
170 wide on 194 pitch; for `n` cards the i-th centre (0-based) is:

```
x = 640 - (n * 194) / 2 + 97 + (i * 194)
```

Verified: n=4 gives 349, 543, 737, 931; n=3 gives 446, 640, 834.

## Gotchas

These all cost real time in this container.

- **The game writes to the real `~/.scoundrel/`.** Driving a game *to completion*
  appends to `runs.log` and can unlock achievements in the player's actual
  history. Abandoning mid-run (kill the app) records nothing - runs persist only
  at game end. Back up `runs.log` / `achievements.log` before automating full games.
- **The window moves between launches.** Observed at screen 449,174 then 531,167
  then 320,144. The driver recomputes the client origin on every invocation -
  never hardcode screen coordinates.
- **The first synthesised click gets swallowed** activating the window, unless
  the caller forces foreground properly. `drive.ps1` does the
  `AttachThreadInput` dance for this; a naive `SetForegroundWindow` is refused
  and costs you a click.
- **Toggle keys carry state between calls.** `S` (slow motion), `R` and `Tab` flip
  whatever the game is already in, and the game outlives a `drive.ps1` call. A
  second chain that "turns slow motion on" actually turns it off, and every
  timed capture then lands in the wrong place — which looks exactly like the
  effect being broken. Either do the whole sequence in one chain, or read the
  on-screen state (the ROOM line prints `[SLOW 1/8]`) before assuming.
- **Do the whole capture in one `drive.ps1` call.** Windows refuses
  `SetForegroundWindow` from a background process, so once the game loses focus
  the driver cannot get it back and exits `NOT_FOREGROUND` until a human clicks
  the window. The game *has* focus right after `lwjgl3:run`, so a single long
  action chain — toggle, trigger, and every `shot:` — works unattended, while
  splitting the same steps across several calls stalls partway. Chain
  aggressively; there is no cost to a long `-Actions` string.
- **The same swallowing hits `key:`, and worse right after launch.** A single
  `key:F9` in the first invocation after `lwjgl3:run` was dropped twice during
  this skill's own verification — the window exists well before the game loop is
  reading input. Send it twice (`key:F9,wait:600,key:F9`); the toggles that
  matter are guarded against re-entry, so a second press is a no-op rather than
  an undo. **Always confirm the screen actually changed** before measuring
  anything in the shot — a dropped key leaves you screenshotting the title screen
  and measuring the wrong pixels.
- **`Shot` uses the client size read at invocation start.** If an action resizes
  the window mid-run (any `key:F11`), the following `shot:` captures a stale
  rectangle. Take the screenshot in a *separate* `drive.ps1` call after a resize.
- **`SetProcessDPIAware()` is mandatory** before any coordinate work, or Windows
  reports scaled sizes and every click lands off-target.
- **`FindWindow` races window creation** during startup; the driver falls back to
  matching `MainWindowTitle` over the process list.
- **Scene2D world Y points up, client Y points down**: `client_y = 720 - world_y`
  when converting a coordinate read off the UI code.
- **Cards resolve on _press_, buttons on _release_** (`Widgets.pressListener`).
  The driver sends a full down+up, which satisfies both.
- **A card with more than one legal move opens a chooser popup** centred on that
  card (e.g. "Barehanded" / "Use weapon" for a monster while armed). Scripted
  play needs a second click to pick one.
- **A disabled button silently does nothing** - e.g. Avoid is permanently
  disabled in Relentless mode, and disabled mid-room in every mode.
- **Action strings split on the first colon only**, because screenshot paths
  contain `C:\`.
- **Screenshots capture screen pixels, not the window's buffer.** Anything
  floating above the window lands in the PNG - a desktop notification toast
  occluded the bottom-right corner during this skill's own verification run. If
  a region looks wrong, re-take the shot before believing it.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `WINDOW_NOT_FOUND`, exit 2 | App isn't up. Re-run with `-WaitSeconds 240`; check `/tmp/scoundrel-run.log` for a Gradle/JVM failure. |
| Screenshot identical after a click | You clicked a disabled control, or the click landed in a gap between cards. Re-read the previous screenshot and re-derive the coordinate. |
| `SHOT C` and a stray file named `C` | You passed the path through a splitter that broke on `C:\`. Use the `shot:` action as documented. |
| Window found but screenshot shows another app | Something is covering it. The driver activates first; just re-run the action. |
