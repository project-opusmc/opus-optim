package org.polydevs.opusmc.optim.platform.mc189;

/** Float-exact M5 box rejection. Plane rows must remain stable during a query. */
public final class FrustumMath {
    private static final float LIMIT = 1.0e10f;
    private FrustumMath() { }

    public static boolean visible(float[][] planes, double x0, double y0, double z0,
            double x1, double y1, double z1) {
        float lx = (float) x0, ly = (float) y0, lz = (float) z0;
        float hx = (float) x1, hy = (float) y1, hz = (float) z1;
        boolean regular = lx <= hx && ly <= hy && lz <= hz
                && lx >= -LIMIT && hx <= LIMIT && ly >= -LIMIT && hy <= LIMIT && lz >= -LIMIT && hz <= LIMIT;
        for (int i = 0; i < 6; i++) {
            float[] p = planes[i];
            float a = p[0], b = p[1], c = p[2], d = p[3];
            if (regular && Math.abs(a) <= LIMIT && Math.abs(b) <= LIMIT && Math.abs(c) <= LIMIT && Math.abs(d) <= LIMIT) {
                float x = a >= 0 ? hx : lx, y = b >= 0 ? hy : ly, z = c >= 0 ? hz : lz;
                if (a * x + b * y + c * z + d <= 0) { return false; }
            } else if (!anyCorner(a, b, c, d, lx, ly, lz, hx, hy, hz)) { return false; }
        }
        return true;
    }

    private static boolean anyCorner(float a, float b, float c, float d,
            float lx, float ly, float lz, float hx, float hy, float hz) {
        // The negated <= deliberately preserves M5's NaN-accepting comparisons.
        return !(a * lx + b * ly + c * lz + d <= 0) || !(a * hx + b * ly + c * lz + d <= 0)
                || !(a * lx + b * hy + c * lz + d <= 0) || !(a * hx + b * hy + c * lz + d <= 0)
                || !(a * lx + b * ly + c * hz + d <= 0) || !(a * hx + b * ly + c * hz + d <= 0)
                || !(a * lx + b * hy + c * hz + d <= 0) || !(a * hx + b * hy + c * hz + d <= 0);
    }
}
