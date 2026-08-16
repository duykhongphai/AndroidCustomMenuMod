package com.nguyen.nebulamenu.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

public final class MenuProfileTest {
    @Test
    public void buildsImmutableProfileTree() {
        MenuProfile profile = MenuProfile.builder("developer", "TOOLS")
                .tab(MenuTab.builder("main", "MAIN")
                        .section(MenuSection.builder("Controls")
                                .control(MenuControl.toggle(
                                        "show_grid",
                                        "Layout grid",
                                        "Show guides",
                                        false
                                ))
                                .build())
                        .build())
                .build();

        assertEquals("developer", profile.getId());
        assertEquals("show_grid", profile.getTabs().get(0)
                .getSections().get(0)
                .getControls().get(0)
                .getId());
    }

    @Test
    public void rejectsDuplicateControlIds() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> MenuProfile.builder("duplicate", "TOOLS")
                        .tab(MenuTab.builder("main", "MAIN")
                                .section(MenuSection.builder("First")
                                        .control(MenuControl.action("export", "EXPORT"))
                                        .build())
                                .build())
                        .tab(MenuTab.builder("other", "OTHER")
                                .section(MenuSection.builder("Second")
                                        .control(MenuControl.action("export", "EXPORT AGAIN"))
                                        .build())
                                .build())
                        .build()
        );

        assertEquals("Duplicate control id: export", error.getMessage());
    }

    @Test
    public void rejectsInvalidSliderRange() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MenuControl.slider("size", "Size", "Invalid", 1f, 1f, 1f)
        );
    }
}
