<#
  Drives the running Scoundrel desktop window (libGDX / LWJGL3) from outside:
  screenshots the client area and sends real mouse clicks via Win32.

  Why this exists: `gradlew lwjgl3:run` opens a native OS window. There is no
  DOM, no accessibility tree, and no built-in remote control, so the only way an
  agent can verify UI work is to screenshot pixels and synthesise clicks.

  Usage (see SKILL.md for the full flow):
    drive.ps1 -WaitSeconds 240 -Actions "shot:C:\out\title.png"
    drive.ps1 -Actions "click:640:346,wait:1000,shot:C:\out\picker.png"
    drive.ps1 -Kill

  Coordinates are CLIENT pixels, origin TOP-LEFT. The window is configured
  1280x720 and the game's FitViewport world is also 1280x720, so they map 1:1 --
  but Scene2D's world Y points UP while client Y points DOWN:
      client_y = 720 - world_y
#>
param(
  [string]$Actions = "",
  [int]$WaitSeconds = 0,
  [switch]$Kill
)

$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing
Add-Type @"
using System;
using System.Runtime.InteropServices;
public class W {
  [DllImport("user32.dll")] public static extern bool SetProcessDPIAware();
  [DllImport("user32.dll", CharSet=CharSet.Auto)] public static extern IntPtr FindWindow(string c, string n);
  [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
  [DllImport("user32.dll")] public static extern uint GetWindowThreadProcessId(IntPtr h, IntPtr pid);
  [DllImport("kernel32.dll")] public static extern uint GetCurrentThreadId();
  [DllImport("user32.dll")] public static extern bool AttachThreadInput(uint a, uint b, bool attach);
  [DllImport("user32.dll")] public static extern bool BringWindowToTop(IntPtr h);
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
  [DllImport("user32.dll")] public static extern bool ShowWindow(IntPtr h, int n);
  [DllImport("user32.dll")] public static extern bool GetClientRect(IntPtr h, out RECT r);
  [DllImport("user32.dll")] public static extern bool ClientToScreen(IntPtr h, ref POINT p);
  [DllImport("user32.dll")] public static extern bool SetCursorPos(int x, int y);
  [DllImport("user32.dll")] public static extern void mouse_event(uint f, uint dx, uint dy, uint d, IntPtr e);
  public struct RECT { public int Left, Top, Right, Bottom; }
  public struct POINT { public int X, Y; }
}
"@

# Must precede any coordinate work: without it Windows lies about sizes and
# positions on a scaled display, and every click lands in the wrong place.
[void][W]::SetProcessDPIAware()

function Get-GameWindow {
  # FindWindow races the window's creation during startup; the process list is
  # the reliable fallback once the JVM is up.
  $h = [W]::FindWindow($null, "Scoundrel")
  if ($h -ne [IntPtr]::Zero) { return $h }
  $p = Get-Process -ErrorAction SilentlyContinue |
       Where-Object { $_.MainWindowTitle -eq "Scoundrel" } | Select-Object -First 1
  if ($p) { return $p.MainWindowHandle }
  return [IntPtr]::Zero
}

if ($Kill) {
  $procs = Get-Process -ErrorAction SilentlyContinue |
           Where-Object { $_.MainWindowTitle -eq "Scoundrel" }
  if ($procs) { $procs | Stop-Process -Force; Write-Output "KILLED" }
  else { Write-Output "NOT_RUNNING" }
  exit 0
}

$h = Get-GameWindow
if ($h -eq [IntPtr]::Zero -and $WaitSeconds -gt 0) {
  $deadline = (Get-Date).AddSeconds($WaitSeconds)
  do {
    Start-Sleep -Milliseconds 2000
    $h = Get-GameWindow
  } while ($h -eq [IntPtr]::Zero -and (Get-Date) -lt $deadline)
}
if ($h -eq [IntPtr]::Zero) { Write-Output "WINDOW_NOT_FOUND"; exit 2 }

# Plain SetForegroundWindow is refused when the caller isn't the foreground
# process; without this the first synthesised click is swallowed activating the
# window instead of pressing the button under it.
function Activate([IntPtr]$hwnd) {
  # Windows can still refuse the raise (foreground lock, another app grabbing
  # focus). One attempt is not enough in practice, so try a few times.
  for ($i = 0; $i -lt 5; $i++) {
    if ([W]::GetForegroundWindow() -eq $hwnd) { return $true }
    $tidFg = [W]::GetWindowThreadProcessId([W]::GetForegroundWindow(), [IntPtr]::Zero)
    $tidMe = [W]::GetCurrentThreadId()
    [void][W]::AttachThreadInput($tidMe, $tidFg, $true)
    [void][W]::ShowWindow($hwnd, 9)   # SW_RESTORE
    [void][W]::BringWindowToTop($hwnd)
    [void][W]::SetForegroundWindow($hwnd)
    [void][W]::AttachThreadInput($tidMe, $tidFg, $false)
    Start-Sleep -Milliseconds 400
  }
  return ([W]::GetForegroundWindow() -eq $hwnd)
}

# Refuse to work blind. If the game is not actually in front, clicks land on
# whatever is (an IDE, a browser) and screenshots capture that instead -- which
# looks like a game that ignored the input rather than like a targeting failure,
# and has twice produced confident measurements of the wrong window.
if (-not (Activate $h)) {
  Write-Output "NOT_FOREGROUND - the Scoundrel window could not be raised; refusing to click or capture"
  exit 3
}

$rect = New-Object W+RECT
[void][W]::GetClientRect($h, [ref]$rect)
$origin = New-Object W+POINT
$origin.X = 0; $origin.Y = 0
[void][W]::ClientToScreen($h, [ref]$origin)
$cw = $rect.Right - $rect.Left
$ch = $rect.Bottom - $rect.Top
Write-Output ("CLIENT {0}x{1} at screen {2},{3}" -f $cw, $ch, $origin.X, $origin.Y)

function Shot([string]$path) {
  $dir = Split-Path -Parent $path
  if ($dir -and -not (Test-Path $dir)) { New-Item -ItemType Directory -Force $dir | Out-Null }
  $bmp = New-Object System.Drawing.Bitmap $cw, $ch
  $g = [System.Drawing.Graphics]::FromImage($bmp)
  $g.CopyFromScreen($origin.X, $origin.Y, 0, 0, (New-Object System.Drawing.Size $cw, $ch))
  $g.Dispose()
  $bmp.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
  $bmp.Dispose()
  Write-Output ("SHOT {0}" -f $path)
}

function Click([int]$x, [int]$y) {
  [void][W]::SetCursorPos($origin.X + $x, $origin.Y + $y)
  Start-Sleep -Milliseconds 150
  [W]::mouse_event(0x0002, 0, 0, 0, [IntPtr]::Zero)   # LEFTDOWN
  Start-Sleep -Milliseconds 90
  [W]::mouse_event(0x0004, 0, 0, 0, [IntPtr]::Zero)   # LEFTUP
  Start-Sleep -Milliseconds 120
  Write-Output ("CLICK {0},{1}" -f $x, $y)
}

foreach ($a in $Actions.Split(",")) {
  if ($a -eq "") { continue }
  # Split on the FIRST colon only -- screenshot paths contain "C:\".
  if ($a.StartsWith("shot:")) {
    Shot $a.Substring(5)
  } elseif ($a.StartsWith("click:")) {
    $c = $a.Substring(6).Split(":")
    Click ([int]$c[0]) ([int]$c[1])
  } elseif ($a.StartsWith("wait:")) {
    Start-Sleep -Milliseconds ([int]$a.Substring(5))
    Write-Output ("WAIT {0}" -f $a.Substring(5))
  } else {
    Write-Output ("UNKNOWN ACTION {0}" -f $a)
  }
}
