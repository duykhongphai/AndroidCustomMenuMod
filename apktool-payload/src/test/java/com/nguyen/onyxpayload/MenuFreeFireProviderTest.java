package com.nguyen.onyxpayload;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.nguyen.onyxmenu.model.MenuControl;
import com.nguyen.onyxmenu.model.MenuProfile;
import com.nguyen.onyxmenu.model.MenuSection;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class MenuFreeFireProviderTest {
    @Test
    public void definesExactlyNineDisabledUiToggles() {
        MenuProfile profile = new MenuFreeFireProvider().createProfile(null);
        List<MenuControl> controls = new ArrayList<>();
        for (MenuSection section : profile.getTabs().get(0).getSections()) {
            controls.addAll(section.getControls());
        }

        assertEquals("menufreefire", profile.getId());
        assertEquals("MENUFREEFIRE", profile.getTitle());
        assertEquals(9, controls.size());
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
                        "Hỗ trợ kéo tâm"
                ),
                titlesOf(controls)
        );
        for (MenuControl control : controls) {
            assertEquals(MenuControl.Type.TOGGLE, control.getType());
            assertFalse(control.isDefaultEnabled());
        }
    }

    private static List<String> titlesOf(List<MenuControl> controls) {
        List<String> titles = new ArrayList<>();
        for (MenuControl control : controls) {
            titles.add(control.getTitle());
        }
        return titles;
    }
}
