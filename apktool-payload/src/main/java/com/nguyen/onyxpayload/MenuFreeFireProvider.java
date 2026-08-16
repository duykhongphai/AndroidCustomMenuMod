package com.nguyen.onyxpayload;

import android.content.Context;

import com.nguyen.onyxmenu.bridge.FeatureBridge;
import com.nguyen.onyxmenu.engine.MenuProvider;
import com.nguyen.onyxmenu.model.MenuControl;
import com.nguyen.onyxmenu.model.MenuProfile;
import com.nguyen.onyxmenu.model.MenuSection;
import com.nguyen.onyxmenu.model.MenuTab;

/**
 * UI-only profile used to preview toggle layout and preference persistence.
 */
public final class MenuFreeFireProvider implements MenuProvider {
    @Override
    public MenuProfile createProfile(Context context) {
        return MenuProfile.builder("menufreefire", "MENUFREEFIRE")
                .subtitle("UI PREVIEW")
                .version("v1.0.0")
                .footer("CHỈ KIỂM TRA GIAO DIỆN")
                .tab(MenuTab.builder("main", "MENU")
                        .section(MenuSection.builder("ESP")
                                .meta("4 TÙY CHỌN")
                                .control(toggle("esp_enabled", "Bật ESP"))
                                .control(toggle("tracer_line", "Đường Kẻ"))
                                .control(toggle("esp_box", "Khung ESP"))
                                .control(toggle("health_bar", "Thanh Máu"))
                                .build())
                        .section(MenuSection.builder("AIM")
                                .meta("5 TÙY CHỌN")
                                .control(toggle("aimbot_enabled", "Bật Aimbot"))
                                .control(toggle("skip_knock", "Bỏ qua Knock"))
                                .control(toggle("silent_aim", "Aim Silent"))
                                .control(toggle("legit_aim", "Aim Legit"))
                                .control(toggle("drag_aim_assist", "Hỗ trợ kéo tâm"))
                                .build())
                        .build())
                .build();
    }

    @Override
    public FeatureBridge createBridge(Context context) {
        return new FeatureBridge() {
            // Intentionally empty: this profile only previews the UI.
        };
    }

    private static MenuControl toggle(String id, String title) {
        return MenuControl.toggle(id, title, "", false);
    }
}
