package org.polydevs.opusmc.optim.core;

import org.junit.jupiter.api.Test;
import org.polydevs.opusmc.optim.core.render.GlyphIndex;
import static org.junit.jupiter.api.Assertions.*;

class GlyphMemoryTest {
    @Test void sparseUnicodeAlphabetFitsThreeKiBAndKeepsEveryLookup() {
        String alphabet = "abcabc\u0000\u20ac\u25a0\uffff";
        short[] table = GlyphIndex.create(alphabet);
        assertTrue(table.length * 2 <= 3072, "Sparse alphabet must not reserve the Unicode gaps");
        for (int c = 0; c <= 65535; c++) {
            assertEquals(alphabet.indexOf(c), GlyphIndex.lookup(table, c), "Code point " + c);
        }
        assertEquals(-1, GlyphIndex.lookup(table, Integer.MIN_VALUE));
        assertEquals(-1, GlyphIndex.lookup(table, Integer.MAX_VALUE));
    }

    @Test void denseUnicodePagesPreserveUnsignedOffsetsAndSurrogateMisses() {
        StringBuilder alphabet = new StringBuilder();
        for (int c = 0; c <= 65535; c += 2) {
            if (!Character.isSurrogate((char) c)) alphabet.append((char) c);
        }
        short[] table = GlyphIndex.create(alphabet.toString());
        assertEquals(0, GlyphIndex.lookup(table, 0));
        assertEquals(31743, GlyphIndex.lookup(table, 65534));
        assertEquals(-1, GlyphIndex.lookup(table, 65535));
        assertEquals(-1, GlyphIndex.lookup(table, 0xd800));
        assertEquals(-1, GlyphIndex.lookup(table, 0xdfff));
        assertTrue(table.length * 2 <= 131072, "Dense alphabet must stay within the original BMP budget");
    }
}
