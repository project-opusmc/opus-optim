package org.polydevs.opusmc.optim.core.render;

import java.util.Arrays;
import java.util.Objects;

/** Immutable BMP alphabet index; callers own an opaque table. No font metrics are cached. */
public final class GlyphIndex {
    private GlyphIndex() { }
    public static short[] create(String alphabet) {
        Objects.requireNonNull(alphabet, "alphabet");
        if (alphabet.length() > Short.MAX_VALUE) { throw new IllegalArgumentException("Alphabet is too long"); }
        boolean[] pages = new boolean[256];
        int pageCount = 0;
        for (int i = 0; i < alphabet.length(); i++) {
            char c = alphabet.charAt(i);
            if (Character.isSurrogate(c)) { throw new IllegalArgumentException("Alphabet must contain BMP characters"); }
            int page = c >>> 8;
            if (page != 0 && !pages[page]) { pages[page] = true; pageCount++; }
        }
        // ASCII/Latin-1 stays a single direct read. High code points use only
        // populated 256-character pages; directory entries are unsigned offsets.
        short[] table = new short[(pageCount == 0 ? 1 : 2 + pageCount) * 256];
        Arrays.fill(table, (short) -1);
        if (pageCount != 0) {
            Arrays.fill(table, 256, 512, (short) 0);
            int offset = 512;
            for (int page = 1; page < 256; page++) {
                if (pages[page]) { table[256 + page] = (short) offset; offset += 256; }
            }
        }
        for (int i = 0; i < alphabet.length(); i++) {
            char c = alphabet.charAt(i);
            int slot = c < 256 ? c : (table[256 + (c >>> 8)] & 0xffff) + (c & 255);
            if (table[slot] == -1) { table[slot] = (short) i; }
        }
        return table;
    }
    public static int lookup(short[] table, int codePoint) {
        if ((codePoint & ~255) == 0) { return table[codePoint]; }
        if ((codePoint & ~65535) != 0 || table.length == 256) { return -1; }
        int offset = table[256 + (codePoint >>> 8)] & 0xffff;
        return offset == 0 ? -1 : table[offset + (codePoint & 255)];
    }
}
