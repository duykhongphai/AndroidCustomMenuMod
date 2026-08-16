package com.nguyen.onyxpayload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import com.nguyen.onyxmenu.model.MenuControl;
import com.nguyen.onyxmenu.model.MenuProfile;
import com.nguyen.onyxmenu.model.MenuSection;
import com.nguyen.onyxmenu.model.MenuTab;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class MenuFreeFireProviderTest {
    @Test
    public void definesGroupedNativeControls() {
        MenuProfile profile = new MenuFreeFireProvider().createProfile(null);
        List<MenuControl> controls = new ArrayList<>();
        for (MenuTab tab : profile.getTabs()) {
            for (MenuSection section : tab.getSections()) {
                controls.addAll(section.getControls());
            }
        }

        assertEquals("menufreefire", profile.getId());
        assertEquals("MENUFREEFIRE", profile.getTitle());
        assertEquals(
                Arrays.asList("ESP", "AIMBOT", "XOAY", "BYPASS"),
                Arrays.asList(
                        profile.getTabs().get(0).getLabel(),
                        profile.getTabs().get(1).getLabel(),
                        profile.getTabs().get(2).getLabel(),
                        profile.getTabs().get(3).getLabel()
                )
        );
        assertEquals(12, controls.size());
        assertEquals(
                Arrays.asList(
                        "Bật ESP",
                        "Đường Kẻ",
                        "Khung ESP",
                        "Thanh Máu",
                        "Bật Aimbot",
                        "Bỏ qua Knock",
                        "Aim Silent",
                        "Aim Legit",
                        "Hỗ trợ kéo tâm",
                        "Xoay",
                        "Tốc độ xoay",
                        "Bypass Emulator Detect"
                ),
                titlesOf(controls)
        );

        int toggleCount = 0;
        for (MenuControl control : controls) {
            if (control.getType() == MenuControl.Type.TOGGLE) {
                toggleCount++;
                assertFalse(control.isDefaultEnabled());
            }
        }
        assertEquals(11, toggleCount);

        MenuControl rotationSpeed = findById(controls, "rotation_speed");
        assertNotNull(rotationSpeed);
        assertEquals(MenuControl.Type.SLIDER, rotationSpeed.getType());
        assertEquals(1f, rotationSpeed.getMinimum(), 0f);
        assertEquals(10f, rotationSpeed.getMaximum(), 0f);
        assertEquals(5f, rotationSpeed.getDefaultValue(), 0f);
    }

    private static List<String> titlesOf(List<MenuControl> controls) {
        List<String> titles = new ArrayList<>();
        for (MenuControl control : controls) {
            titles.add(control.getTitle());
        }
        return titles;
    }

    private static MenuControl findById(List<MenuControl> controls, String id) {
        for (MenuControl control : controls) {
            if (id.equals(control.getId())) {
                return control;
            }
        }
        return null;
    }
}
