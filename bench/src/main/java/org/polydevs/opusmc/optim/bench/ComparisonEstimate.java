package org.polydevs.opusmc.optim.bench;

/** Unequal-variance interval for absolute run-mean difference, in observed control-mean units. */
public final class ComparisonEstimate {
    // NIST 0.975 table rounded to 3 decimals, each increased by .001 to avoid rounding downward.
    private static final double[] T95 = {12.707, 4.304, 3.183, 2.777, 2.572, 2.448, 2.366, 2.307,
        2.263, 2.229, 2.202, 2.180, 2.161, 2.146, 2.132, 2.121, 2.111, 2.102, 2.094, 2.087,
        2.081, 2.075, 2.070, 2.065, 2.061, 2.057, 2.053, 2.049, 2.046, 2.043};
    private final double controlMeanNanos, treatmentMeanNanos, effectPercent, lowerPercent, upperPercent, noiseFloorPercent;
    ComparisonEstimate(double[] aaControl, double[] aaRepeat, double[] control, double[] treatment) {
        double[] aa = interval(aaControl, aaRepeat), ab = interval(control, treatment);
        controlMeanNanos = ab[0]; treatmentMeanNanos = ab[1];
        effectPercent = 100 * (ab[0] - ab[1]) / ab[0];
        lowerPercent = 100 * (ab[0] - ab[1] - ab[2]) / ab[0];
        upperPercent = 100 * (ab[0] - ab[1] + ab[2]) / ab[0];
        noiseFloorPercent = 100 * (Math.abs(aa[0] - aa[1]) + aa[2]) / aa[0];
    }
    private static double[] interval(double[] control, double[] treatment) {
        double[] a = moments(control), b = moments(treatment);
        int df = Math.min(30, Math.min(control.length, treatment.length) - 1);
        double margin = T95[df - 1] * Math.sqrt(a[1] / control.length + b[1] / treatment.length);
        return new double[]{a[0], b[0], margin};
    }
    private static double[] moments(double[] values) {
        double mean = 0, m2 = 0; int count = 0;
        for (double value : values) {
            count++; double delta = value - mean; mean += delta / count; m2 += delta * (value - mean);
        }
        return new double[]{mean, Math.max(0, m2 / (count - 1))};
    }
    public double controlMeanNanos() { return controlMeanNanos; }
    public double treatmentMeanNanos() { return treatmentMeanNanos; }
    public double effectPercent() { return effectPercent; }
    public double lowerPercent() { return lowerPercent; }
    public double upperPercent() { return upperPercent; }
    public double noiseFloorPercent() { return noiseFloorPercent; }
}
