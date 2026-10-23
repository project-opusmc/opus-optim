package org.polydevs.opusmc.optim.core;

import org.junit.jupiter.api.Test;
import org.polydevs.opusmc.optim.core.render.*;
import org.polydevs.opusmc.optim.core.workload.DeadlineBudget;
import static org.junit.jupiter.api.Assertions.*;

class PipelineKernelsTest {
    @Test void chunkInitialBudgetPreservesSmallAndInvalidRequestsWithoutCappingGrowth() {
        assertEquals(-1, BufferGrowth.initialChunkInts(-1));
        assertEquals(0, BufferGrowth.initialChunkInts(0));
        assertEquals(64, BufferGrowth.initialChunkInts(64));
        assertEquals(8192, BufferGrowth.initialChunkInts(Integer.MAX_VALUE));
        assertEquals(40*1024*1024, BufferGrowth.capacity(40*1024*1024,32768));
    }
    @Test void glyphTableKeepsFirstDuplicateAndMissingCodePoints() {
        short[] table = GlyphIndex.create("AABA\u0000ç");
        assertEquals(0, GlyphIndex.lookup(table, 'A'));
        assertEquals(2, GlyphIndex.lookup(table, 'B'));
        assertEquals(4, GlyphIndex.lookup(table, 0));
        assertEquals(5, GlyphIndex.lookup(table, 'ç'));
        assertEquals(-1, GlyphIndex.lookup(table, 'C'));
        assertEquals(-1, GlyphIndex.lookup(table, -1));
        assertEquals(-1, GlyphIndex.lookup(table, 0x10000));
        assertEquals(-1, GlyphIndex.lookup(GlyphIndex.create(""), 'A'));
        assertTrue(table.length <= 65536);
    }
    @Test void glyphTableMatchesIndependentSearchForEveryBmpCodePoint() {
        String alphabet = "abcabc\u0000\u00a0\u042f\u03a9\uffff";
        short[] table = GlyphIndex.create(alphabet);
        for (int c = 0; c <= 65535; c++) { assertEquals(alphabet.indexOf(c), GlyphIndex.lookup(table, c)); }
        assertThrows(IllegalArgumentException.class, () -> GlyphIndex.create("\ud800"));
        assertThrows(IllegalArgumentException.class, () -> GlyphIndex.create("\ud83d\ude00"));
        assertThrows(IllegalArgumentException.class, () -> GlyphIndex.create(new String(new char[32768])));
    }
    @Test void bufferGrowthReducesReallocationWithExplicitHeadroomCeiling() {
        int m = 1024 * 1024;
        assertEquals(8*m, BufferGrowth.capacity(6*m, 4*m));
        assertEquals(32*m, BufferGrowth.capacity(22*m, 20*m));
        assertEquals(40*m, BufferGrowth.capacity(40*m, 20*m));
        assertEquals(42*m, BufferGrowth.capacity(42*m, 40*m));
        assertEquals(2*m, BufferGrowth.capacity(2*m, 0));
        assertEquals(-100, BufferGrowth.capacity(-100, 20*m));
        assertEquals(1, BufferGrowth.capacity(1, Integer.MAX_VALUE));
    }
    @Test void deadlineCapPreservesSingleTaskSentinelExpiredAndShorterDeadline() {
        assertEquals(0, DeadlineBudget.cap(0, 100, 20));
        assertEquals(90, DeadlineBudget.cap(90, 100, 20));
        assertEquals(115, DeadlineBudget.cap(115, 100, 20));
        assertEquals(120, DeadlineBudget.cap(150, 100, 20));
        assertEquals(Long.MIN_VALUE+9, DeadlineBudget.cap(Long.MIN_VALUE+189, Long.MAX_VALUE-10, 20));
        assertThrows(IllegalArgumentException.class, () -> DeadlineBudget.cap(1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> DeadlineBudget.cap(1, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> DeadlineBudget.cap(1, 0, 1_000_000_001));
    }
}
