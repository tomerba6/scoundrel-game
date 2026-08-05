package com.tomer.scoundrel.screens;

/**
 * Tips a sprite over in whole-pixel stages, for the bottle pouring into the
 * health bar.
 *
 * <p>The reference mock turns the bottle 62°, which the browser anti-aliases
 * into hundreds of colours that are nowhere in the palette. Every other rule in
 * this art forbids exactly that, so the bottle leans instead: each row shifts
 * sideways by a whole number of pixels. A pixel never changes colour, only
 * which column it sits in — so the tilt is lossless by construction, and the
 * lean still reads as pouring.
 */
final class TiltMask {

    /** How many discrete lean stages there are, each held for a frame. */
    static final int STAGES = 3;

    /**
     * How far the top leans at the strongest stage, as a fraction of the
     * sprite's height. Proportional rather than a fixed pixel count, so the
     * lean is the same angle whatever is being tipped.
     */
    private static final float MAX_LEAN = 0.28f;

    private TiltMask() {
    }

    /**
     * How far row {@code y} of a sprite {@code height} tall shifts at this
     * stage. Rows above the pivot go one way and rows below the other, so the
     * sprite tips about its middle rather than sliding.
     */
    static int shiftAt(int stage, int y, int height) {
        if (stage <= 0 || height <= 1) {
            return 0;
        }
        float lean = height * MAX_LEAN * Math.min(stage, STAGES) / STAGES;
        float fromPivot = (height / 2f - y) / (height / 2f);
        return Math.round(lean * fromPivot);
    }

    /**
     * @param argb   source pixels, row-major, alpha in the high byte
     * @param width  source width in pixels
     * @param height source height in pixels
     * @param stage  0 for upright, up to {@link #STAGES} for fully tipped
     * @return a new array the same size, leaning; pixels pushed past an edge are dropped
     */
    static int[] tilt(int[] argb, int width, int height, int stage) {
        int[] out = new int[argb.length];
        if (stage <= 0) {
            System.arraycopy(argb, 0, out, 0, argb.length);
            return out;
        }
        for (int y = 0; y < height; y++) {
            int shift = shiftAt(stage, y, height);
            for (int x = 0; x < width; x++) {
                int target = x + shift;
                if (target < 0 || target >= width) {
                    continue;
                }
                out[y * width + target] = argb[y * width + x];
            }
        }
        return out;
    }
}
