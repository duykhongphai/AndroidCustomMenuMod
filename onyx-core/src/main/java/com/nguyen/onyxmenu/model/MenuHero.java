package com.nguyen.onyxmenu.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class MenuHero {
    private final String eyebrow;
    private final String heading;
    private final String description;
    private final List<MenuMetric> metrics;

    private MenuHero(Builder builder) {
        eyebrow = requireText(builder.eyebrow, "eyebrow");
        heading = requireText(builder.heading, "heading");
        description = requireText(builder.description, "description");
        metrics = Collections.unmodifiableList(new ArrayList<>(builder.metrics));
    }

    public static Builder builder(String eyebrow, String heading) {
        return new Builder(eyebrow, heading);
    }

    public String getEyebrow() {
        return eyebrow;
    }

    public String getHeading() {
        return heading;
    }

    public String getDescription() {
        return description;
    }

    public List<MenuMetric> getMetrics() {
        return metrics;
    }

    public static final class Builder {
        private final String eyebrow;
        private final String heading;
        private String description = "Customizable menu profile";
        private final List<MenuMetric> metrics = new ArrayList<>();

        private Builder(String eyebrow, String heading) {
            this.eyebrow = eyebrow;
            this.heading = heading;
        }

        public Builder description(String value) {
            description = value;
            return this;
        }

        public Builder metric(String value, String label) {
            metrics.add(MenuMetric.of(value, label));
            return this;
        }

        public MenuHero build() {
            return new MenuHero(this);
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
