package com.nguyen.nebulamenu.model;

import java.util.Objects;

public final class MenuMetric {
    private final String value;
    private final String label;

    private MenuMetric(String value, String label) {
        this.value = requireText(value, "value");
        this.label = requireText(label, "label");
    }

    public static MenuMetric of(String value, String label) {
        return new MenuMetric(value, label);
    }

    public String getValue() {
        return value;
    }

    public String getLabel() {
        return label;
    }

    private static String requireText(String value, String field) {
        String result = Objects.requireNonNull(value, field).trim();
        if (result.isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be empty");
        }
        return result;
    }
}
