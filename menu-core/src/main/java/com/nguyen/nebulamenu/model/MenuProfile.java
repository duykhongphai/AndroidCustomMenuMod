package com.nguyen.nebulamenu.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class MenuProfile {
    private final String id;
    private final String title;
    private final String subtitle;
    private final String version;
    private final String footer;
    private final List<MenuTab> tabs;

    private MenuProfile(Builder builder) {
        id = requireText(builder.id, "id");
        title = requireText(builder.title, "title");
        subtitle = requireText(builder.subtitle, "subtitle");
        version = requireText(builder.version, "version");
        footer = requireText(builder.footer, "footer");
        tabs = Collections.unmodifiableList(new ArrayList<>(builder.tabs));
        if (tabs.isEmpty()) {
            throw new IllegalArgumentException("Profile " + id + " requires tabs");
        }
        validateUniqueIds();
    }

    public static Builder builder(String id, String title) {
        return new Builder(id, title);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getVersion() {
        return version;
    }

    public String getFooter() {
        return footer;
    }

    public List<MenuTab> getTabs() {
        return tabs;
    }

    private void validateUniqueIds() {
        Set<String> tabIds = new HashSet<>();
        Set<String> controlIds = new HashSet<>();
        for (MenuTab tab : tabs) {
            if (!tabIds.add(tab.getId())) {
                throw new IllegalArgumentException("Duplicate tab id: " + tab.getId());
            }
            for (MenuSection section : tab.getSections()) {
                for (MenuControl control : section.getControls()) {
                    if (!controlIds.add(control.getId())) {
                        throw new IllegalArgumentException("Duplicate control id: " + control.getId());
                    }
                }
            }
        }
    }

    public static final class Builder {
        private final String id;
        private final String title;
        private String subtitle = "CONTROL CENTER";
        private String version = "v1.0.0";
        private String footer = "LOCAL PROFILE";
        private final List<MenuTab> tabs = new ArrayList<>();

        private Builder(String id, String title) {
            this.id = id;
            this.title = title;
        }

        public Builder subtitle(String value) {
            subtitle = value;
            return this;
        }

        public Builder version(String value) {
            version = value;
            return this;
        }

        public Builder footer(String value) {
            footer = value;
            return this;
        }

        public Builder tab(MenuTab value) {
            tabs.add(Objects.requireNonNull(value, "tab"));
            return this;
        }

        public MenuProfile build() {
            return new MenuProfile(this);
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
