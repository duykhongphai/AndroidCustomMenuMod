package com.nguyen.nebulamenu.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class MenuControl {
    public enum Type {
        TOGGLE,
        SLIDER,
        ACTION,
        PALETTE
    }

    private final Type type;
    private final String id;
    private final String title;
    private final String description;
    private final boolean defaultEnabled;
    private final float minimum;
    private final float maximum;
    private final float defaultValue;
    private final boolean danger;
    private final String defaultOptionId;
    private final List<MenuOption> options;

    private MenuControl(Builder builder) {
        type = Objects.requireNonNull(builder.type, "type");
        id = requireText(builder.id, "id");
        title = requireText(builder.title, "title");
        description = builder.description == null ? "" : builder.description.trim();
        defaultEnabled = builder.defaultEnabled;
        minimum = builder.minimum;
        maximum = builder.maximum;
        defaultValue = builder.defaultValue;
        danger = builder.danger;
        defaultOptionId = builder.defaultOptionId;
        options = Collections.unmodifiableList(new ArrayList<>(builder.options));

        if (type == Type.SLIDER && (!(minimum < maximum)
                || defaultValue < minimum
                || defaultValue > maximum)) {
            throw new IllegalArgumentException("Invalid slider range for " + id);
        }
        if (type == Type.PALETTE) {
            if (options.isEmpty()) {
                throw new IllegalArgumentException("Palette " + id + " requires options");
            }
            boolean hasDefault = false;
            for (MenuOption option : options) {
                if (option.getId().equals(defaultOptionId)) {
                    hasDefault = true;
                    break;
                }
            }
            if (!hasDefault) {
                throw new IllegalArgumentException("Palette default option is missing for " + id);
            }
        }
    }

    public static MenuControl toggle(
            String id,
            String title,
            String description,
            boolean defaultEnabled
    ) {
        return new Builder(Type.TOGGLE, id, title)
                .description(description)
                .defaultEnabled(defaultEnabled)
                .build();
    }

    public static MenuControl slider(
            String id,
            String title,
            String description,
            float minimum,
            float maximum,
            float defaultValue
    ) {
        return new Builder(Type.SLIDER, id, title)
                .description(description)
                .range(minimum, maximum, defaultValue)
                .build();
    }

    public static MenuControl action(String id, String title) {
        return new Builder(Type.ACTION, id, title).build();
    }

    public static MenuControl dangerAction(String id, String title) {
        return new Builder(Type.ACTION, id, title).danger(true).build();
    }

    public static MenuControl palette(
            String id,
            String title,
            String defaultOptionId,
            MenuOption... options
    ) {
        return new Builder(Type.PALETTE, id, title)
                .defaultOption(defaultOptionId)
                .options(Arrays.asList(options))
                .build();
    }

    public Type getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public boolean isDefaultEnabled() {
        return defaultEnabled;
    }

    public float getMinimum() {
        return minimum;
    }

    public float getMaximum() {
        return maximum;
    }

    public float getDefaultValue() {
        return defaultValue;
    }

    public boolean isDanger() {
        return danger;
    }

    public String getDefaultOptionId() {
        return defaultOptionId;
    }

    public List<MenuOption> getOptions() {
        return options;
    }

    private static final class Builder {
        private final Type type;
        private final String id;
        private final String title;
        private String description = "";
        private boolean defaultEnabled;
        private float minimum;
        private float maximum = 1f;
        private float defaultValue = 0.5f;
        private boolean danger;
        private String defaultOptionId;
        private List<MenuOption> options = Collections.emptyList();

        private Builder(Type type, String id, String title) {
            this.type = type;
            this.id = id;
            this.title = title;
        }

        private Builder description(String value) {
            description = value;
            return this;
        }

        private Builder defaultEnabled(boolean value) {
            defaultEnabled = value;
            return this;
        }

        private Builder range(float min, float max, float defaultAmount) {
            minimum = min;
            maximum = max;
            defaultValue = defaultAmount;
            return this;
        }

        private Builder danger(boolean value) {
            danger = value;
            return this;
        }

        private Builder defaultOption(String value) {
            defaultOptionId = value;
            return this;
        }

        private Builder options(List<MenuOption> value) {
            options = value;
            return this;
        }

        private MenuControl build() {
            return new MenuControl(this);
        }
    }

    private static String requireText(String value, String field) {
        String result = Objects.requireNonNull(value, field).trim();
        if (result.isEmpty()) {
            throw new IllegalArgumentException(field + " cannot be empty");
        }
        return result;
    }
}
