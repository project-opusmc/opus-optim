package org.polydevs.opusmc.optim.bench;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FrameStatisticsTest {
    @Test void computesRunPercentilesAndSlowestFrameMeansRatherThanUsingP99AsALow() {
        long[] frames = new long[1000];
        for (int i = 0; i < frames.length; i++) { frames[i] = (i + 1) * 1_000_000L; }
        FrameStatistics stats = FrameStatistics.of(frames);
        assertEquals(500_500_000, stats.meanNanos());
        assertEquals(500_000_000, stats.percentileNanos(.50));
        assertEquals(950_000_000, stats.percentileNanos(.95));
        assertEquals(990_000_000, stats.percentileNanos(.99));
        assertEquals(1000.0 / 995.5, stats.onePercentLowFps().getAsDouble(), 1e-12);
        assertEquals(1.0, stats.pointOnePercentLowFps().getAsDouble());
    }
    @Test void leavesLowFpsAbsentUntilItsMinimumSampleCount() {
        assertFalse(FrameStatistics.of(Fixtures.frames(99, 10)).onePercentLowFps().isPresent());
        assertTrue(FrameStatistics.of(Fixtures.frames(100, 10)).onePercentLowFps().isPresent());
        assertFalse(FrameStatistics.of(Fixtures.frames(999, 10)).pointOnePercentLowFps().isPresent());
    }
    @Test void avoidsLongSumOverflowAndDoesNotSortTheCallersArray() {
        long[] input = {Long.MAX_VALUE, Long.MAX_VALUE - 2};
        FrameStatistics stats = FrameStatistics.of(input);
        assertEquals((double) Long.MAX_VALUE, stats.meanNanos());
        assertArrayEquals(new long[]{Long.MAX_VALUE, Long.MAX_VALUE - 2}, input);
        assertEquals(Long.MAX_VALUE - 2, stats.percentileNanos(.5));
    }
    @Test void rejectsEmptyInvalidDurationsAndInvalidPercentiles() {
        assertThrows(IllegalArgumentException.class, () -> FrameStatistics.of(new long[0]));
        assertThrows(IllegalArgumentException.class, () -> FrameStatistics.of(new long[]{0}));
        assertThrows(IllegalArgumentException.class, () -> FrameStatistics.of(new long[]{-1}));
        FrameStatistics stats = FrameStatistics.of(new long[]{1});
        for (double p : new double[]{0, -1, 1.01, Double.NaN}) {
            assertThrows(IllegalArgumentException.class, () -> stats.percentileNanos(p));
        }
    }
    @Test void decimalPercentileBoundariesDoNotRoundIntoTheNextRank() {
        long[] frames = new long[100];
        for (int i = 0; i < 100; i++) { frames[i] = i + 1; }
        FrameStatistics stats = FrameStatistics.of(frames);
        assertEquals(7, stats.percentileNanos(.07));
        assertEquals(14, stats.percentileNanos(.14));
        assertEquals(28, stats.percentileNanos(.28));
    }
}
