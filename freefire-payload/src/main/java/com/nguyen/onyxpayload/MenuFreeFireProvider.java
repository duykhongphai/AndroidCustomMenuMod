package com.nguyen.onyxpayload;

import android.content.Context;

import com.nguyen.onyxmenu.bridge.FeatureBridge;
import com.nguyen.onyxmenu.engine.MenuProvider;
import com.nguyen.onyxmenu.model.MenuControl;
import com.nguyen.onyxmenu.model.MenuProfile;
import com.nguyen.onyxmenu.model.MenuSection;
import com.nguyen.onyxmenu.model.MenuTab;

/** Profile menufreefire có bridge JNI tới trạng thái C++ trong cùng tiến trình. */
public final class MenuFreeFireProvider implements MenuProvider {
    @Override
    public MenuProfile createProfile(Context context) {
        return MenuProfile.builder("menufreefire", "MENUFREEFIRE")
                .subtitle("ĐIỀU KHIỂN NATIVE")
                .version("v1.2.0")
                .footer("JNI · C++ · NỘI BỘ TIẾN TRÌNH")
                .tab(MenuTab.builder("esp", "ESP")
                        .section(MenuSection.builder("Hiển thị")
                                .meta("4 TÙY CHỌN")
                                .control(toggle("esp_enabled", "Bật ESP"))
                                .control(toggle("tracer_line", "Đường Kẻ"))
                                .control(toggle("esp_box", "Khung ESP"))
                                .control(toggle("health_bar", "Thanh Máu"))
                                .build())
                        .build())
                .tab(MenuTab.builder("aimbot", "AIMBOT")
                        .section(MenuSection.builder("Hỗ trợ ngắm")
                                .meta("5 TÙY CHỌN")
                                .control(toggle("aimbot_enabled", "Bật Aimbot"))
                                .control(toggle("skip_knock", "Bỏ qua Knock"))
                                .control(toggle("silent_aim", "Aim Silent"))
                                .control(toggle("legit_aim", "Aim Legit"))
                                .control(toggle("drag_aim_assist", "Hỗ trợ kéo tâm"))
                                .build())
                        .build())
                .tab(MenuTab.builder("rotation", "XOAY")
                        .section(MenuSection.builder("Điều khiển xoay")
                                .meta("TỐC ĐỘ 1–10")
                                .control(toggle("rotation_enabled", "Xoay"))
                                .control(MenuControl.slider(
                                        "rotation_speed",
                                        "Tốc độ xoay",
                                        "Tùy chỉnh tốc độ xoay từ 1 đến 10",
                                        1f,
                                        10f,
                                        5f
                                ))
                                .build())
                        .build())
                .tab(MenuTab.builder("bypass", "BYPASS")
                        .section(MenuSection.builder("Tương thích")
                                .meta("CHỈ LƯU TRẠNG THÁI")
                                .control(toggle(
                                        "bypass_emulator_detect",
                                        "Bypass Emulator Detect"
                                ))
                                .build())
                        .build())
                .build();
    }

    @Override
    public FeatureBridge createBridge(Context context) {
        return new MenuFreeFireFeatureBridge(context);
    }

    private static MenuControl toggle(String id, String title) {
        return MenuControl.toggle(id, title, "", false);
    }
}
