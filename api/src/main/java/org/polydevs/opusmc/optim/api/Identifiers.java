package org.polydevs.opusmc.optim.api;

import java.util.Objects;

public final class Identifiers {
    private Identifiers() { }

    public static String requireValid(String id) {
        if (id == null || !id.matches("[a-z0-9][a-z0-9._-]{0,127}")) {
            throw new IllegalArgumentException("Invalid Optim identifier: " + id);
        }
        return id;
    }

    static String requireText(String value, String field) {
        Objects.requireNonNull(value, field);
        if (value.trim().isEmpty()) { throw new IllegalArgumentException(field + " must be nonempty"); }
        return value;
    }
}
