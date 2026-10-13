package org.polydevs.opusmc.optim.core.render;

import java.util.Arrays;
import java.util.Objects;

/** Immutable BMP alphabet index; callers own the returned table. No font metrics are cached. */
public final class GlyphIndex {
    private GlyphIndex() { }
    public static short[] create(String alphabet) {
        Objects.requireNonNull(alphabet, "alphabet");
        if (alphabet.length() > Short.MAX_VALUE) { throw new IllegalArgumentException("Alphabet is too long"); }
        int max = -1;
        for (int i = 0; i < alphabet.length(); i++) {
            char c = alphabet.charAt(i);
            if (Character.isSurrogate(c)) { throw new IllegalArgumentException("Alphabet must contain BMP characters"); }
            max = Math.max(max, c);
        }
        short[] table = new short[max + 1];
        Arrays.fill(table, (short) -1);
        for (int i = 0; i < alphabet.length(); i++) {
            char c = alphabet.charAt(i);
            if (table[c] == -1) { table[c] = (short) i; }
        }
        return table;
    }
    public static int lookup(short[] table, int codePoint) {
        return codePoint >= 0 && codePoint < table.length ? table[codePoint] : -1;
    }
}
