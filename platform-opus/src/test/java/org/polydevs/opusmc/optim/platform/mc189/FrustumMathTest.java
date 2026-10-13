package org.polydevs.opusmc.optim.platform.mc189;

import java.util.Random;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FrustumMathTest {
    @Test void touchingPlanesRemainCulledAndCoefficientSignsChooseTheRightCorner() {
        float[][] planes = acceptingPlanes(); planes[0] = new float[]{1, 0, 0, 0};
        assertFalse(FrustumMath.visible(planes, -1, -1, -1, 0, 1, 1));
        assertTrue(FrustumMath.visible(planes, -1, -1, -1, 1, 1, 1));
        planes[0] = new float[]{-1, 0, 0, 0};
        assertTrue(FrustumMath.visible(planes, -1, -1, -1, 1, 1, 1));
        assertFalse(FrustumMath.visible(planes, 0, -1, -1, 1, 1, 1));
    }
    @Test void preservesM5FloatCastingAtRepresentableBoundaries() {
        float[][] planes = acceptingPlanes(); planes[0] = new float[]{1, 0, 0, -16_777_216};
        assertFalse(FrustumMath.visible(planes, 16_777_216, 0, 0, 16_777_217, 1, 1));
        assertTrue(FrustumMath.visible(planes, 16_777_216, 0, 0, 16_777_218, 1, 1));
    }
    @Test void normalizedPlanesAndOrderedBoxesMatchIndependentEightCornerOracle() {
        Random random = new Random(20261013);
        for (int trial = 0; trial < 50_000; trial++) {
            float[][] planes = new float[6][4];
            for (float[] p : planes) {
                for (int c = 0; c < 3; c++) { p[c] = random.nextFloat() * 2 - 1; }
                p[3] = random.nextFloat() * 100_000 - 50_000;
            }
            double x = random.nextDouble() * 100_000 - 50_000, y = random.nextDouble() * 100_000 - 50_000;
            double z = random.nextDouble() * 100_000 - 50_000;
            compare(planes, x, y, z, x + random.nextDouble() * 1000,
                    y + random.nextDouble() * 1000, z + random.nextDouble() * 1000);
        }
    }
    @Test void cancellationAndSubnormalRoundingMatchTheOriginalComparison() {
        float[] coefficients = {0f, -0f, Float.MIN_VALUE, -Float.MIN_VALUE, 1f, -1f, Math.nextUp(1f), Math.nextDown(1f)};
        double[] coordinates = {0, -0d, Float.MIN_VALUE, -Float.MIN_VALUE, 1, -1, 16_777_217, -16_777_217};
        for (float a : coefficients) {
            for (float b : coefficients) {
                for (double x : coordinates) {
                    float[][] planes = acceptingPlanes(); planes[0] = new float[]{a, b, -a, -b};
                    compare(planes, x, x, x, Math.nextUp(x), Math.nextUp(x), Math.nextUp(x));
                }
            }
        }
    }
    @Test void nonfiniteOverflowAndReversedInputsKeepTheExactSlowPath() {
        double[] edge = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY,
                Double.MAX_VALUE, -Double.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, 0, -0d, 1};
        float[] coefficients = {Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
                Float.MAX_VALUE, -Float.MAX_VALUE, 1, -1, 0, -0f};
        for (double min : edge) {
            for (double max : edge) {
                for (float a : coefficients) {
                    float[][] planes = acceptingPlanes(); planes[0] = new float[]{a, -a, a, -a};
                    compare(planes, min, min, min, max, max, max);
                }
            }
        }
        Random random = new Random(189);
        for (int i = 0; i < 20_000; i++) {
            float[][] planes = new float[6][4];
            for (float[] p : planes) { for (int j = 0; j < 4; j++) { p[j] = Float.intBitsToFloat(random.nextInt()); } }
            double[] box = new double[6];
            for (int j = 0; j < 6; j++) { box[j] = Float.intBitsToFloat(random.nextInt()); }
            compare(planes, box[0], box[1], box[2], box[3], box[4], box[5]);
        }
    }
    @Test void malformedMatricesKeepSequentialAccessAndEarlyReturn() {
        assertThrows(NullPointerException.class, () -> FrustumMath.visible(null, 0, 0, 0, 1, 1, 1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> FrustumMath.visible(new float[0][], 0, 0, 0, 1, 1, 1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> FrustumMath.visible(new float[][]{new float[3]}, 0, 0, 0, 1, 1, 1));
        float[][] early = new float[][]{new float[]{0, 0, 0, -1}, null};
        assertFalse(FrustumMath.visible(early, 0, 0, 0, 1, 1, 1));
    }
    private static void compare(float[][] planes, double x0, double y0, double z0, double x1, double y1, double z1) {
        assertEquals(oracle(planes, x0, y0, z0, x1, y1, z1), FrustumMath.visible(planes, x0, y0, z0, x1, y1, z1));
    }
    static float[][] acceptingPlanes() {
        float[][] planes = new float[6][4]; for (float[] plane : planes) { plane[3] = 1; } return planes;
    }
    // Independent exhaustive corner enumeration, using the original M5 NaN comparison.
    static boolean oracle(float[][] planes, double x0, double y0, double z0, double x1, double y1, double z1) {
        float[] lo = {(float) x0, (float) y0, (float) z0}, hi = {(float) x1, (float) y1, (float) z1};
        for (int plane = 0; plane < 6; plane++) {
            float[] p = planes[plane]; boolean any = false;
            for (int corner = 0; corner < 8; corner++) {
                float x = (corner & 1) == 0 ? lo[0] : hi[0], y = (corner & 2) == 0 ? lo[1] : hi[1];
                float z = (corner & 4) == 0 ? lo[2] : hi[2];
                if (!(p[0] * x + p[1] * y + p[2] * z + p[3] <= 0)) { any = true; break; }
            }
            if (!any) { return false; }
        }
        return true;
    }
}
