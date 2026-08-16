package com.nguyen.onyxmenu.model;

import java.util.Objects;

public final class MenuOption {
    private final String id;
    private final String label;
    private final int color;

    private MenuOption(String id, String label, int color) {
        this.id = requireText(id, "id");
        this.label = requireText(label, "label");
        this.color = color;
    }

    public static MenuOption color(String id, String label, int color) {
        return new MenuOption(id, label, color);
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public int getColor() {
        return color;
    }

    private static String requireText(String value, String field) {
        String result = Objects.requireNonNull(value, field).trim();
        if (result.isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be empty");
        }
        return result;
    }
}
