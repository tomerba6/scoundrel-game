"""The recipe for every sound effect: what it is meant to sound like, and how it is built.

Written without being heard - Claude cannot listen - so each recipe says in its
docstring what it is *meant* to be, and the listening rounds judge whether it
is. The numbers are starting points to be tuned by ear.

The file names mirror the Java contract, `com.tomer.scoundrel.audio.Sound`:
`<sound>_<version>`, or `<sound>_<weight>_<version>` for a weighted sound.
AudioAssetsTest holds the folder to that enum, and check.py holds this list to
the folder, so the three cannot drift apart silently.

Weights index 0 = light, 1 = medium, 2 = heavy. The thud has no medium.
"""

import zlib
from dataclasses import dataclass, field

import numpy as np

import synth as s

WEIGHTS = ("light", "medium", "heavy")
FRAME = 1 / 12
"""One effect frame. The bare-handed blows land a frame apart, and so do the deal's cards."""


# --- the recipes -------------------------------------------------------------

def click(rng, w, v):
    """A menu button going down: a small, dry, wooden 'tock' - a damped knock with a
    tick of grit on the front. Quiet; it is the only sound on most menu screens."""
    n = s.samples(0.07)
    body = s.modal(n, [(1150, 1.0, 0.035), (2320, 0.35, 0.02), (3480, 0.15, 0.012)])
    tick = s.bandpass(s.noise(n, rng), 3200, q=1.0) * s.decay(n, 0.006)
    return body + 0.5 * tick


def flip(rng, w, v):
    """A card from the dungeon landing in the room: a papery snap with a soft pat of the
    card meeting the table. Short - four of them land a frame (83 ms) apart."""
    n = s.samples(0.085)
    centre = (2400, 2900, 2100)[v - 1] * (1 + 0.05 * rng.uniform(-1, 1))
    snap = s.bandpass(s.noise(n, rng), centre, q=0.9) * s.attack_decay(n, 0.0008, 0.045)
    pat = s.sweep_sine(n, 260, 140, 0.02) * s.attack_decay(n, 0.001, 0.035)
    return snap + 0.35 * pat


def sweep(rng, w, v):
    """A whole room swept back into the dungeon: four cards whooshing away together, a
    band of noise sliding down as it goes, with a papery flutter. The cards are gone
    in 250 ms, so is the sound."""
    n = s.samples(0.34)
    t = s.times(n)
    centres = 3200 * (700 / 3200) ** (t / t[-1])
    whoosh = s.swept_bandpass(s.noise(n, rng), centres, q=1.3)
    swell = np.clip(t / 0.03, 0, 1) * np.exp(-np.maximum(t - 0.03, 0) * s.LN_1000 / 0.3)
    flutter = 1 + 0.35 * s.one_pole_lowpass(s.noise(n, rng), 40) * 6
    return whoosh * swell * np.clip(flutter, 0.3, 1.7)


def equip(rng, w, v):
    """A weapon set down on the rail: an iron clank - the inharmonic ring of a struck bar -
    with a short scrape in its attack. A shiv rings high and brief; a greatsword rings
    low and long, with a dull thump of weight under it."""
    base = (1250, 820, 520)[w]
    t60 = (0.18, 0.30, 0.48)[w]
    n = s.samples(t60 + 0.08)
    ratios, amps, decays = (1.0, 2.76, 5.40, 8.93), (1.0, 0.6, 0.35, 0.2), (1.0, 0.6, 0.4, 0.25)
    partials = [(base * r * (1 + 0.004 * rng.uniform(-1, 1)), a, t60 * d)
                for r, a, d in zip(ratios, amps, decays)]
    ring = s.modal(n, partials)
    scrape = s.highpass(s.noise(n, rng), 2500) * s.attack_decay(n, 0.001, 0.05)
    thump = s.sweep_sine(n, 160, 60, 0.03) * s.attack_decay(n, 0.001, 0.15)
    return ring + 0.4 * scrape + (0.0, 0.3, 0.6)[w] * thump


def blade(rng, w, v):
    """A weapon kill, the blade landing: a hiss of steel through the air that ends in a
    short bright ring. Thin and high for a light weapon; lower, longer, with a chop of
    impact under it for a heavy one."""
    n = s.samples((0.26, 0.34, 0.44)[w])
    t = s.times(n)
    hiss_band = 6000 * (2000 / 6000) ** np.clip(t / 0.06, 0, 1)
    hiss_band *= 1 + 0.1 * rng.uniform(-1, 1)
    hiss = s.swept_bandpass(s.noise(n, rng), hiss_band, q=0.8) \
        * s.attack_decay(n, 0.0008, (0.07, 0.09, 0.11)[w])
    base = (2600, 1900, 1300)[w] * (1 + 0.03 * rng.uniform(-1, 1))
    t60 = (0.12, 0.18, 0.26)[w]
    ring = s.modal(n, [(base, 1.0, t60), (base * 1.51, 0.55, t60 * 0.7),
                       (base * 2.3, 0.3, t60 * 0.5), (base * 3.7, 0.15, t60 * 0.35)])
    chop = s.sweep_sine(n, 200, 70, 0.02) * s.attack_decay(n, 0.001, 0.08)
    return hiss + 0.55 * ring + (0.0, 0.35, 0.6)[w] * chop


def thud(rng, w, v):
    """Damage getting through the weapon: a dull body blow under the blade, felt more
    than heard. Heavier when more got through. Absent on a clean kill."""
    heavy = w == 2
    n = s.samples(0.38 if heavy else 0.22)
    f_start, f_end = (110, 45) if heavy else (150, 70)
    # The second version differs by design, not by chance: a low sine is most of
    # this sound, and a random few-percent detune left the two near-identical.
    detune, drop, grit_band = ((1.0, 0.04, 500), (0.86, 0.06, 750))[v - 1]
    f_start *= detune * (1 + 0.02 * rng.uniform(-1, 1))
    body = s.sweep_sine(n, f_start, f_end * detune, drop) * s.attack_decay(n, 0.002, 0.3 if heavy else 0.15)
    grit = s.lowpass(s.noise(n, rng), grit_band) * s.attack_decay(n, 0.001, 0.05)
    return s.saturate(body + 0.3 * grit, 2.5 if heavy else 1.5)


def fist(rng, w, v):
    """A bare-handed fight: two blows a frame (83 ms) apart, each a low punch with a slap
    of skin on top. The heavier the monster, the deeper and longer they land. Both
    blows are in the one file, matching the two stars."""
    t60 = (0.09, 0.12, 0.16)[w]
    f_start, f_end = ((190, 80), (150, 65), (115, 50))[w]
    slap_band = (2200, 1600, 1100)[w]
    # Versions differ by design: pitch, how fast the punch drops, the spacing and
    # balance of the two blows. A random few-percent detune left them near-identical.
    detune, drop, late, second_strength = ((1.0, 0.025, 0.0, 0.85), (0.9, 0.04, 0.006, 1.0))[v - 1]
    gap = s.samples(FRAME + late)
    n = gap + s.samples(t60 + 0.06)

    def blow(strength, shift):
        m = s.samples(t60 + 0.06)
        k = detune * shift
        punch = s.sweep_sine(m, f_start * k, f_end * k, drop) * s.attack_decay(m, 0.001, t60)
        slap = s.bandpass(s.noise(m, rng), slap_band * k, q=0.8) * s.attack_decay(m, 0.0005, 0.018)
        return strength * (punch + 0.6 * slap)

    first = blow(1.0, 1 + 0.02 * rng.uniform(-1, 1))
    second = blow(second_strength, 0.93 + 0.02 * rng.uniform(-1, 1))
    x = s.place(n, first, 0) + s.place(n, second, gap)
    return s.saturate(x, (1.2, 1.6, 2.2)[w])


def drink(rng, w, v):
    """A potion pouring: a quick run of glugs - bubbles, each a tone that rises as it dies -
    over a soft wash of liquid. A dram is three small high bubbles; a flagon is six
    deeper ones and a longer pour."""
    count = (3, 4, 6)[w]
    lo, hi = ((600, 900), (450, 750), (320, 600))[w]
    spacing = (0.06, 0.07, 0.08)[w]
    n = s.samples(count * spacing + 0.16)
    x = np.zeros(n)
    at = 0
    for i in range(count):
        f0 = rng.uniform(lo, hi)
        b = s.bubble(s.samples(0.08), f0, rise=8.0, t60=rng.uniform(0.03, 0.05))
        x += s.place(n, b * (1.0 if i == 0 else rng.uniform(0.6, 0.95)), at)
        at += s.samples(spacing * rng.uniform(0.8, 1.2))
    t = s.times(n)
    wash = s.lowpass(s.noise(n, rng), 1200) * np.clip(t / 0.02, 0, 1) * np.exp(-t * s.LN_1000 / (n / s.SR))
    return x + 0.08 * wash


def spill(rng, w, v):
    """A wasted potion tipping over where it stood: a small glass tink as the bottle goes
    over, then a thin dribble - drained and quiet, because nothing reaches you."""
    n = s.samples(0.5)
    tink = s.modal(n, [(3100, 1.0, 0.12), (7300, 0.4, 0.06), (10100, 0.2, 0.04)])
    x = 0.6 * tink
    for at, f0 in ((0.12, 1300), (0.2, 1000), (0.27, 1150)):
        x += 0.35 * s.place(n, s.bubble(s.samples(0.06), f0, rise=6.0, t60=0.035), s.samples(at))
    splash = s.lowpass(s.noise(n, rng), 1800) * s.place(n, s.decay(s.samples(0.15), 0.12), s.samples(0.1))
    return s.lowpass(x + 0.15 * splash, 3500)


def chime(rng, w, v):
    """Trophies unlocked: two warm bell strikes a fifth apart, D5 then A5 - the key the
    music is in. A little ceremony, not a fanfare."""
    n = s.samples(1.7)

    def bell(fundamental):
        # hum, prime, tierce (minor third), quint, nominal: a church-bell's partials
        return s.modal(s.samples(1.7), [
            (fundamental * 0.5, 0.35, 1.4), (fundamental, 1.0, 1.1), (fundamental * 1.2, 0.45, 0.8),
            (fundamental * 1.5, 0.3, 0.6), (fundamental * 2.0, 0.35, 0.5), (fundamental * 2.74, 0.12, 0.3)],
            attack=0.001)

    return s.place(n, bell(587.33), 0) + 0.8 * s.place(n, bell(880.0), s.samples(0.14))


# --- the catalogue -----------------------------------------------------------

@dataclass(frozen=True)
class Recipe:
    sound: str
    build: object
    versions: int
    weights: tuple = ()
    target_db: tuple = ()
    """Loudness per weight (or one value), as the loudest 50 ms in dBFS."""
    max_seconds: float = 1.0
    lofi: dict = field(default_factory=dict)


# Loudness targets are relative to one another: the board's impacts sit around -16,
# the flips and the click well below, since they come four at a time or on every
# menu press. Heavier weights are a little louder as well as deeper.
RECIPES = (
    Recipe("click", click, 1, target_db=(-24,), max_seconds=0.12),
    Recipe("flip", flip, 3, target_db=(-24,), max_seconds=0.10),
    Recipe("sweep", sweep, 1, target_db=(-22,), max_seconds=0.45),
    Recipe("equip", equip, 1, WEIGHTS, (-18, -17, -16), max_seconds=0.8),
    Recipe("blade", blade, 2, WEIGHTS, (-17, -16, -15), max_seconds=0.6, lofi={"cutoff": 9500}),
    Recipe("thud", thud, 2, ("light", "heavy"), (-20, -18), max_seconds=0.5,
           lofi={"hold_rate": 14000, "cutoff": 6000}),
    Recipe("fist", fist, 2, WEIGHTS, (-17, -16, -15), max_seconds=0.6,
           lofi={"hold_rate": 16000, "cutoff": 7000}),
    Recipe("drink", drink, 1, WEIGHTS, (-20, -19, -18), max_seconds=0.8),
    Recipe("spill", spill, 1, target_db=(-21,), max_seconds=0.8),
    Recipe("chime", chime, 1, target_db=(-18,), max_seconds=2.0, lofi={"cutoff": 10000, "bits": 11}),
)


@dataclass(frozen=True)
class Job:
    """One file to render."""
    name: str
    recipe: Recipe
    weight: int
    version: int
    target_db: float


def jobs():
    """Every sound-effect file, in the Java enum's order, with the parameters to build it."""
    out = []
    for recipe in RECIPES:
        if recipe.weights:
            for weight_name, target in zip(recipe.weights, recipe.target_db):
                w = WEIGHTS.index(weight_name)
                for v in range(1, recipe.versions + 1):
                    out.append(Job(f"{recipe.sound}_{weight_name}_{v}", recipe, w, v, target))
        else:
            for v in range(1, recipe.versions + 1):
                out.append(Job(f"{recipe.sound}_{v}", recipe, 0, v, recipe.target_db[0]))
    return out


def render(job):
    """The finished float signal for one file. Seeded by its name, so it never changes
    unless its recipe does."""
    rng = np.random.default_rng(zlib.crc32(job.name.encode("utf-8")))
    raw = job.recipe.build(rng, job.weight, job.version)
    return s.finish(raw, job.target_db, lofi_args=job.recipe.lofi)
