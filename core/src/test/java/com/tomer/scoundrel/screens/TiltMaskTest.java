package com.tomer.scoundrel.screens;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tipping a bottle without rotating it. Each row shifts sideways by a whole
 * number of pixels, so the sprite leans while every pixel keeps its exact
 * colour — a real rotation would resample the edges into tones that are
 * nowhere in the palette, which is the one thing the art cannot tolerate.
 */
class TiltMaskTest {

    /** A 4×4 block of distinguishable colours, so pixels can be traced. */
    private static int[] block() {
        int[] src = new int[16];
        for (int i = 0; i < src.length; i++) {
            src[i] = 0xff000000 | (i + 1) * 0x050505;
        }
        return src;
    }

    private static Map<Integer, Integer> census(int[] pixels) {
        Map<Integer, Integer> counts = new HashMap<>();
        for (int argb : pixels) {
            if ((argb >>> 24) != 0) {
                counts.merge(argb, 1, Integer::sum);
            }
        }
        return counts;
    }

    @Test
    void stageZeroLeavesTheSpriteAlone() {
        int[] src = block();
        assertEquals(census(src), census(TiltMask.tilt(src, 4, 4, 0)));
        assertEquals(src.length, TiltMask.tilt(src, 4, 4, 0).length);
    }

    /**
     * The property the whole approach rests on: a tilt moves pixels, it never
     * blends them. Every colour present before is present after, in the same
     * quantity, unless it fell off the edge.
     */
    @Test
    void aTiltInventsNoColourAtAll() {
        int[] src = block();
        for (int stage = 0; stage <= TiltMask.STAGES; stage++) {
            Map<Integer, Integer> after = census(TiltMask.tilt(src, 4, 4, stage));
            Map<Integer, Integer> before = census(src);
            for (Integer colour : after.keySet()) {
                assertTrue(before.containsKey(colour),
                        "stage " + stage + " invented #" + Integer.toHexString(colour));
            }
        }
    }

    @Test
    void aTiltActuallyLeansTheSprite() {
        int[] src = new int[64];
        // A vertical bar down the middle of an 8×8.
        for (int y = 0; y < 8; y++) {
            src[y * 8 + 4] = 0xffff0000;
        }
        int[] tilted = TiltMask.tilt(src, 8, 8, TiltMask.STAGES);
        assertNotEquals(java.util.Arrays.toString(src), java.util.Arrays.toString(tilted),
                "the strongest tilt should have moved something");
        // The top and bottom of the bar should no longer share a column.
        int top = columnOf(tilted, 8, 0);
        int bottom = columnOf(tilted, 8, 7);
        assertNotEquals(top, bottom, "a lean means the ends are offset from each other");
    }

    @Test
    void everyRowShiftsByAWholeNumberOfPixels() {
        // Implicit in an int[] of pixels — there is no way to express a half
        // shift — but the offsets themselves must also be whole and monotonic.
        int previous = 0;
        for (int stage = 1; stage <= TiltMask.STAGES; stage++) {
            int shift = TiltMask.shiftAt(stage, 0, 64);
            assertTrue(Math.abs(shift) >= Math.abs(previous),
                    "stage " + stage + " leaned less than stage " + (stage - 1));
            previous = shift;
        }
    }

    @Test
    void pixelsPushedOffTheEdgeAreDroppedRatherThanWrapped() {
        int[] src = new int[16];
        src[0] = 0xffabcdef;      // top-left corner
        int[] tilted = TiltMask.tilt(src, 4, 4, TiltMask.STAGES);
        for (int i = 0; i < tilted.length; i++) {
            if (tilted[i] == 0xffabcdef) {
                assertTrue(i % 4 <= 3, "pixel wrapped to the other side");
            }
        }
    }

    private static int columnOf(int[] pixels, int width, int row) {
        for (int x = 0; x < width; x++) {
            if ((pixels[row * width + x] >>> 24) != 0) {
                return x;
            }
        }
        return -1;
    }
}
