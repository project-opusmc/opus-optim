package org.polydevs.opusmc.optim.bench;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;

final class BenchValues {
    private BenchValues() { }
    static String text(String value) {
        if (value == null || value.trim().isEmpty() || value.length() > 256) {
            throw new IllegalArgumentException("Benchmark identity must contain 1..256 characters");
        }
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (Character.isHighSurrogate(character)) {
                if (i + 1 == value.length() || !Character.isLowSurrogate(value.charAt(i + 1))) {
                    throw new IllegalArgumentException("Malformed Unicode benchmark identity");
                }
                i++;
            } else if (Character.isLowSurrogate(character)) {
                throw new IllegalArgumentException("Malformed Unicode benchmark identity");
            }
        }
        return value;
    }
    static String hash(String value) {
        if (value == null || !value.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("Expected lowercase SHA-256 identity");
        }
        return value;
    }
    static String fingerprint(Map<String, String> sortedFields) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            sortedFields.forEach((key, value) -> {
                update(digest, key); update(digest, value);
            });
            StringBuilder result = new StringBuilder(64);
            for (byte b : digest.digest()) {
                result.append(Character.forDigit((b >>> 4) & 15, 16));
                result.append(Character.forDigit(b & 15, 16));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
    private static void update(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        for (int shift = 24; shift >= 0; shift -= 8) { digest.update((byte) (bytes.length >>> shift)); }
        digest.update(bytes);
    }
}
