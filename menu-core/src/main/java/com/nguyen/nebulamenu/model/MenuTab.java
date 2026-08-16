package com.nguyen.nebulamenu.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class MenuTab {
    private final String id;
    private final String label;
    private final MenuHero hero;
    private final List<MenuSection> sections;

    private MenuTab(Builder builder) {
        id = requireText(builder.id, "id");
        label = requireText(builder.label, "label");
        hero = builder.hero;
        sections = Collections.unmodifiableList(new ArrayList<>(builder.sections));
        if (sections.isEmpty()) {
            throw new IllegalArgumentException("Tab " + id + " requires sections");
        }
    }

    public static Builder builder(String id, String label) {
        return new Builder(id, label);
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public MenuHero getHero() {
        return hero;
    }

    public List<MenuSection> getSections() {
        return sections;
    }

    public static final class Builder {
        private final String id;
        private final String label;
        private MenuHero hero;
        private final List<MenuSection> sections = new ArrayList<>();

        private Builder(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public Builder hero(MenuHero value) {
            hero = value;
            return this;
        }

        public Builder section(MenuSection value) {
            sections.add(Objects.requireNonNull(value, "section"));
            return this;
        }

        public MenuTab build() {
            return new MenuTab(this);
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
