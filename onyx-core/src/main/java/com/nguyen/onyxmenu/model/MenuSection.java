package com.nguyen.onyxmenu.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class MenuSection {
    private final String title;
    private final String meta;
    private final List<MenuControl> controls;

    private MenuSection(Builder builder) {
        title = requireText(builder.title, "title");
        meta = builder.meta == null ? "" : builder.meta.trim();
        controls = Collections.unmodifiableList(new ArrayList<>(builder.controls));
        if (controls.isEmpty()) {
            throw new IllegalArgumentException("Section " + title + " requires controls");
        }
    }

    public static Builder builder(String title) {
        return new Builder(title);
    }

    public String getTitle() {
        return title;
    }

    public String getMeta() {
        return meta;
    }

    public List<MenuControl> getControls() {
        return controls;
    }

    public static final class Builder {
        private final String title;
        private String meta = "";
        private final List<MenuControl> controls = new ArrayList<>();

        private Builder(String title) {
            this.title = title;
        }

        public Builder meta(String value) {
            meta = value;
            return this;
        }

        public Builder control(MenuControl value) {
            controls.add(Objects.requireNonNull(value, "control"));
            return this;
        }

        public MenuSection build() {
            return new MenuSection(this);
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
