"""Renders Scoundrel's synthesized sound effects into assets/audio/sfx/.

    python audio-source/render.py

Deterministic: every file is seeded by its own name, so an unchanged recipe
re-renders to the same bytes, and check.py can prove the committed files are
what the recipes produce.

A file listed in audio-source/replaced.txt has been replaced by a sourced
sound (see docs/audio.md, Sourcing) and is never rendered over.
"""

import io
import sys
import wave
from pathlib import Path

import recipes
import synth

ROOT = Path(__file__).resolve().parent.parent
SFX_DIR = ROOT / "assets" / "audio" / "sfx"
REPLACED_LIST = Path(__file__).resolve().parent / "replaced.txt"


def replaced():
    """File names (without extension) that are no longer ours to render."""
    if not REPLACED_LIST.exists():
        return set()
    names = set()
    for line in REPLACED_LIST.read_text(encoding="utf-8").splitlines():
        line = line.split("#", 1)[0].strip()
        if line:
            names.add(line)
    return names


def wav_bytes(signal):
    """A canonical 16-bit mono WAV at synth.SR: a bare RIFF header, fmt and data, nothing else."""
    buffer = io.BytesIO()
    with wave.open(buffer, "wb") as out:
        out.setnchannels(1)
        out.setsampwidth(2)
        out.setframerate(synth.SR)
        out.writeframes(synth.to_pcm16(signal))
    return buffer.getvalue()


def render_sfx():
    """Every sound effect that is still ours, as {name: wav bytes}."""
    skip = replaced()
    return {job.name: wav_bytes(recipes.render(job)) for job in recipes.jobs() if job.name not in skip}


def main():
    SFX_DIR.mkdir(parents=True, exist_ok=True)
    rendered = render_sfx()
    for name, data in rendered.items():
        (SFX_DIR / f"{name}.wav").write_bytes(data)
    expected = {job.name for job in recipes.jobs()}
    strays = sorted(p.name for p in SFX_DIR.iterdir() if p.stem not in expected or p.suffix != ".wav")
    print(f"rendered {len(rendered)} sound effects into {SFX_DIR.relative_to(ROOT)}"
          f" ({len(expected) - len(rendered)} replaced, left alone)")
    if strays:
        # Not deleted: a stray may be someone's sourced sound. Everything in assets/ ships,
        # so AudioAssetsTest fails until it is dealt with.
        print("files here that no recipe makes (they will ship!):", ", ".join(strays))
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
