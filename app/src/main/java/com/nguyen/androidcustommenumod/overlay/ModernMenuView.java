package com.nguyen.androidcustommenumod.overlay;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import com.nguyen.androidcustommenumod.bridge.FeatureBridge;
import com.nguyen.androidcustommenumod.storage.PreferenceStore;
import com.nguyen.androidcustommenumod.ui.BrandMarkView;
import com.nguyen.androidcustommenumod.ui.Design;
import com.nguyen.androidcustommenumod.ui.ModernSlider;
import com.nguyen.androidcustommenumod.ui.ModernToggle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressLint("ViewConstructor")
public final class ModernMenuView extends LinearLayout {
    private final FeatureBridge bridge;
    private final PreferenceStore preferences;
    private final LinearLayout content;
    private final List<TextView> tabViews = new ArrayList<>();
    private final View dragHandle;
    private Runnable collapseListener;
    private Runnable closeListener;
    private int selectedTab;

    public ModernMenuView(Context context, FeatureBridge bridge, PreferenceStore preferences) {
        super(context);
        this.bridge = bridge;
        this.preferences = preferences;

        setOrientation(VERTICAL);
        setElevation(Design.dp(context, 20));
        setClipToOutline(true);
        setBackground(Design.outlined(context, 0xFC0D111C, 0xFF30384C, 24, 1));

        dragHandle = createHeader(context);
        addView(dragHandle, new LayoutParams(LayoutParams.MATCH_PARENT, Design.dp(context, 76)));

        addView(createTabs(context), new LayoutParams(LayoutParams.MATCH_PARENT, Design.dp(context, 49)));
        addView(divider(context), new LayoutParams(LayoutParams.MATCH_PARENT, Design.dp(context, 1)));

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setVerticalScrollBarEnabled(false);
        content = Design.vertical(context);
        content.setPadding(
                Design.dp(context, 14),
                Design.dp(context, 12),
                Design.dp(context, 14),
                Design.dp(context, 16)
        );
        scrollView.addView(content, new ScrollView.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        ));
        addView(scrollView, new LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f));

        addView(divider(context), new LayoutParams(LayoutParams.MATCH_PARENT, Design.dp(context, 1)));
        addView(createFooter(context), new LayoutParams(LayoutParams.MATCH_PARENT, Design.dp(context, 38)));

        selectTab(0);
    }

    public View getDragHandle() {
        return dragHandle;
    }

    public void setOnCollapseListener(Runnable listener) {
        collapseListener = listener;
    }

    public void setOnCloseListener(Runnable listener) {
        closeListener = listener;
    }

    private View createHeader(Context context) {
        LinearLayout header = Design.horizontal(context);
        header.setPadding(
                Design.dp(context, 16),
                Design.dp(context, 13),
                Design.dp(context, 12),
                Design.dp(context, 11)
        );

        BrandMarkView mark = new BrandMarkView(context);
        header.addView(mark, new LayoutParams(Design.dp(context, 44), Design.dp(context, 44)));

        LinearLayout identity = Design.vertical(context);
        identity.setPadding(Design.dp(context, 11), 0, 0, 0);
        TextView name = Design.title(context, "NEBULA", 15);
        name.setLetterSpacing(0.16f);
        TextView subtitle = Design.text(context, "CONTROL CENTER", 9, Design.MUTED);
        subtitle.setLetterSpacing(0.12f);
        subtitle.setPadding(0, Design.dp(context, 4), 0, 0);
        identity.addView(name, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        identity.addView(subtitle, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        header.addView(identity, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        TextView live = Design.pill(context, "● LIVE", Design.SUCCESS, 0x1F54E6A5);
        header.addView(live, new LayoutParams(LayoutParams.WRAP_CONTENT, Design.dp(context, 28)));

        TextView minimize = Design.iconButton(context, "—", "Collapse menu");
        LayoutParams buttonParams = new LayoutParams(Design.dp(context, 36), Design.dp(context, 36));
        buttonParams.setMargins(Design.dp(context, 8), 0, 0, 0);
        minimize.setOnClickListener(view -> {
            if (collapseListener != null) {
                collapseListener.run();
            }
        });
        header.addView(minimize, buttonParams);

        TextView close = Design.iconButton(context, "×", "Close overlay");
        LayoutParams closeParams = new LayoutParams(Design.dp(context, 36), Design.dp(context, 36));
        closeParams.setMargins(Design.dp(context, 6), 0, 0, 0);
        close.setTextColor(Design.DANGER);
        close.setOnClickListener(view -> {
            if (closeListener != null) {
                closeListener.run();
            }
        });
        header.addView(close, closeParams);
        return header;
    }

    private View createTabs(Context context) {
        LinearLayout tabs = Design.horizontal(context);
        tabs.setPadding(
                Design.dp(context, 12),
                Design.dp(context, 7),
                Design.dp(context, 12),
                Design.dp(context, 6)
        );
        String[] names = {"CORE", "VISUAL", "SYSTEM"};
        for (int index = 0; index < names.length; index++) {
            final int tabIndex = index;
            TextView tab = Design.text(context, names[index], 10, Design.MUTED);
            tab.setGravity(Gravity.CENTER);
            tab.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            tab.setLetterSpacing(0.1f);
            tab.setClickable(true);
            tab.setFocusable(true);
            tab.setOnClickListener(view -> selectTab(tabIndex));
            LayoutParams params = new LayoutParams(0, LayoutParams.MATCH_PARENT, 1f);
            if (index > 0) {
                params.setMargins(Design.dp(context, 5), 0, 0, 0);
            }
            tabs.addView(tab, params);
            tabViews.add(tab);
        }
        return tabs;
    }

    private void selectTab(int index) {
        selectedTab = index;
        for (int tabIndex = 0; tabIndex < tabViews.size(); tabIndex++) {
            TextView tab = tabViews.get(tabIndex);
            boolean active = tabIndex == selectedTab;
            tab.setTextColor(active ? Design.TEXT : Design.MUTED);
            tab.setBackground(active
                    ? Design.gradient(getContext(), 0x4D7C5CFF, 0x242DE2E6, 12)
                    : Design.ripple(0x227C5CFF, Design.rounded(getContext(), Color.TRANSPARENT, 12))
            );
        }
        renderSelectedTab();
    }

    private void renderSelectedTab() {
        content.removeAllViews();
        if (selectedTab == 0) {
            renderCore();
        } else if (selectedTab == 1) {
            renderVisual();
        } else {
            renderSystem();
        }
    }

    private void renderCore() {
        Context context = getContext();
        LinearLayout hero = Design.vertical(context);
        hero.setPadding(
                Design.dp(context, 16),
                Design.dp(context, 15),
                Design.dp(context, 16),
                Design.dp(context, 15)
        );
        GradientDrawable heroBackground = Design.gradient(context, 0xFF24204B, 0xFF122C3A, 18);
        heroBackground.setStroke(Design.dp(context, 1), 0x667C5CFF);
        hero.setBackground(heroBackground);

        LinearLayout heroTop = Design.horizontal(context);
        TextView label = Design.label(context, "Live workspace");
        label.setTextColor(Design.ACCENT_LIGHT);
        heroTop.addView(label, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        heroTop.addView(Design.pill(context, "01", Design.CYAN, 0x1F2DE2E6));
        hero.addView(heroTop);

        TextView title = Design.title(context, "Everything, right where\nyou need it.", 21);
        title.setLineSpacing(0, 0.94f);
        LayoutParams titleParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        titleParams.setMargins(0, Design.dp(context, 12), 0, 0);
        hero.addView(title, titleParams);

        TextView body = Design.text(context, "A polished overlay shell with a clean, replaceable bridge.", 12, 0xFFB9C2D5);
        body.setLineSpacing(Design.dp(context, 3), 1f);
        LayoutParams bodyParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        bodyParams.setMargins(0, Design.dp(context, 9), 0, 0);
        hero.addView(body, bodyParams);
        content.addView(hero, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        LinearLayout stats = Design.horizontal(context);
        LayoutParams statsParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        statsParams.setMargins(0, Design.dp(context, 9), 0, 0);
        content.addView(stats, statsParams);
        stats.addView(statCard("06", "MODULES", Design.ACCENT_LIGHT), new LayoutParams(0, Design.dp(context, 72), 1f));
        LayoutParams secondStat = new LayoutParams(0, Design.dp(context, 72), 1f);
        secondStat.setMargins(Design.dp(context, 9), 0, 0, 0);
        stats.addView(statCard("1.0", "BUILD", Design.CYAN), secondStat);

        addSectionTitle("Quick controls", "DEMO BRIDGE");
        addToggleCard("focus_mode", "Focus mode", "Quiet, distraction-free profile", true);
        addToggleCard("safe_overlay", "Safe overlay", "Keeps the UI touch-friendly", true);
        addToggleCard("edge_glow", "Edge glow", "Subtle accent around active cards", false);
        addSliderCard("intensity", "Interface intensity", "Animation and glow strength", 0.68f);
    }

    private void renderVisual() {
        addIntroBlock(
                "VISUAL SYSTEM",
                "Built to feel native,\nnot generic.",
                "Tune color, atmosphere and motion without changing the layout code."
        );
        addSectionTitle("Accent palette", "5 PRESETS");
        addPalettePicker();
        addSectionTitle("Atmosphere", "LIVE PREVIEW");
        addToggleCard("ambient_light", "Ambient light", "Soft color bloom behind surfaces", true);
        addToggleCard("particle_field", "Particle field", "Adds depth to the background", true);
        addToggleCard("reduced_motion", "Reduced motion", "Uses simpler menu transitions", false);
        addSliderCard("surface_opacity", "Surface opacity", "Balance contrast and depth", 0.92f);
    }

    private void renderSystem() {
        addIntroBlock(
                "LOCAL DEMO",
                "A bridge you can\nreplace safely.",
                "The included implementation only logs UI events. Connect it to software you own."
        );
        addSectionTitle("Behavior", "PREFERENCES");
        addToggleCard("haptic_feedback", "Haptic feedback", "Small response on interactions", true);
        addToggleCard("auto_collapse", "Auto collapse", "Return to the bubble after launch", false);
        addSliderCard("animation_speed", "Animation speed", "Controls interface transitions", 0.72f);

        addSectionTitle("Maintenance", "LOCAL ONLY");
        addActionButton("PING DEMO BRIDGE", false, () -> {
            bridge.onAction("ping_demo_bridge");
            Toast.makeText(getContext(), "Demo event sent to Logcat", Toast.LENGTH_SHORT).show();
        });
        addActionButton("RESTORE DEFAULTS", true, () -> {
            preferences.clearMenuValues();
            bridge.onAction("restore_defaults");
            renderSelectedTab();
            Toast.makeText(getContext(), "Menu preferences restored", Toast.LENGTH_SHORT).show();
        });
    }

    private void addIntroBlock(String eyebrow, String heading, String bodyText) {
        Context context = getContext();
        LinearLayout card = Design.vertical(context);
        card.setPadding(
                Design.dp(context, 16),
                Design.dp(context, 16),
                Design.dp(context, 16),
                Design.dp(context, 16)
        );
        card.setBackground(Design.outlined(context, Design.SURFACE, 0xFF2B344A, 18, 1));
        TextView label = Design.label(context, eyebrow);
        label.setTextColor(Design.CYAN);
        card.addView(label);
        TextView title = Design.title(context, heading, 20);
        title.setLineSpacing(0, 0.96f);
        LayoutParams titleParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        titleParams.setMargins(0, Design.dp(context, 11), 0, 0);
        card.addView(title, titleParams);
        TextView description = Design.text(context, bodyText, 12, Design.MUTED);
        description.setLineSpacing(Design.dp(context, 3), 1f);
        LayoutParams descriptionParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        descriptionParams.setMargins(0, Design.dp(context, 9), 0, 0);
        card.addView(description, descriptionParams);
        content.addView(card);
    }

    private View statCard(String value, String caption, int accent) {
        Context context = getContext();
        LinearLayout card = Design.vertical(context);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(Design.dp(context, 14), 0, Design.dp(context, 14), 0);
        card.setBackground(Design.outlined(context, Design.SURFACE, Design.DIVIDER, 16, 1));
        TextView valueView = Design.title(context, value, 20);
        valueView.setTextColor(accent);
        card.addView(valueView);
        TextView captionView = Design.label(context, caption);
        LayoutParams captionParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        captionParams.setMargins(0, Design.dp(context, 5), 0, 0);
        card.addView(captionView, captionParams);
        return card;
    }

    private void addSectionTitle(String title, String meta) {
        Context context = getContext();
        LinearLayout row = Design.horizontal(context);
        LayoutParams rowParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(Design.dp(context, 2), Design.dp(context, 20), Design.dp(context, 2), Design.dp(context, 8));
        content.addView(row, rowParams);
        TextView titleView = Design.title(context, title, 13);
        row.addView(titleView, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        TextView metaView = Design.label(context, meta);
        metaView.setTextColor(Design.SUBTLE);
        row.addView(metaView);
    }

    private void addToggleCard(String key, String title, String description, boolean defaultValue) {
        Context context = getContext();
        LinearLayout row = Design.horizontal(context);
        row.setPadding(
                Design.dp(context, 14),
                Design.dp(context, 12),
                Design.dp(context, 14),
                Design.dp(context, 12)
        );
        row.setBackground(Design.ripple(
                0x267C5CFF,
                Design.outlined(context, Design.SURFACE, Design.DIVIDER, 16, 1)
        ));
        row.setClickable(true);
        row.setFocusable(true);

        LinearLayout copy = Design.vertical(context);
        TextView titleView = Design.title(context, title, 13);
        TextView descriptionView = Design.text(context, description, 11, Design.MUTED);
        descriptionView.setPadding(0, Design.dp(context, 5), 0, 0);
        copy.addView(titleView);
        copy.addView(descriptionView);
        row.addView(copy, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        ModernToggle toggle = new ModernToggle(context);
        toggle.setContentDescription(title);
        toggle.setChecked(preferences.getBoolean(key, defaultValue));
        toggle.setOnCheckedChangeListener(checked -> {
            preferences.putBoolean(key, checked);
            bridge.onToggleChanged(key, checked);
        });
        LayoutParams toggleParams = new LayoutParams(Design.dp(context, 50), Design.dp(context, 28));
        toggleParams.setMargins(Design.dp(context, 12), 0, 0, 0);
        row.addView(toggle, toggleParams);
        row.setOnClickListener(view -> toggle.performClick());

        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, Design.dp(context, 8));
        content.addView(row, params);
    }

    private void addSliderCard(String key, String title, String description, float defaultValue) {
        Context context = getContext();
        LinearLayout card = Design.vertical(context);
        card.setPadding(
                Design.dp(context, 14),
                Design.dp(context, 13),
                Design.dp(context, 14),
                Design.dp(context, 11)
        );
        card.setBackground(Design.outlined(context, Design.SURFACE, Design.DIVIDER, 16, 1));

        LinearLayout heading = Design.horizontal(context);
        LinearLayout copy = Design.vertical(context);
        TextView titleView = Design.title(context, title, 13);
        TextView descriptionView = Design.text(context, description, 11, Design.MUTED);
        descriptionView.setPadding(0, Design.dp(context, 5), 0, 0);
        copy.addView(titleView);
        copy.addView(descriptionView);
        heading.addView(copy, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        float initialValue = preferences.getFloat(key, defaultValue);
        TextView valueView = Design.pill(context, formatPercent(initialValue), Design.CYAN, 0x1F2DE2E6);
        heading.addView(valueView);
        card.addView(heading);

        ModernSlider slider = new ModernSlider(context);
        slider.setContentDescription(title);
        slider.setValue(initialValue);
        slider.setOnValueChangeListener((value, fromUser) -> {
            valueView.setText(formatPercent(value));
            if (fromUser) {
                preferences.putFloat(key, value);
                bridge.onValueChanged(key, value);
            }
        });
        LayoutParams sliderParams = new LayoutParams(LayoutParams.MATCH_PARENT, Design.dp(context, 38));
        sliderParams.setMargins(0, Design.dp(context, 8), 0, 0);
        card.addView(slider, sliderParams);

        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, Design.dp(context, 8));
        content.addView(card, params);
    }

    private void addPalettePicker() {
        Context context = getContext();
        LinearLayout palette = Design.horizontal(context);
        palette.setGravity(Gravity.CENTER);
        palette.setPadding(
                Design.dp(context, 12),
                Design.dp(context, 14),
                Design.dp(context, 12),
                Design.dp(context, 14)
        );
        palette.setBackground(Design.outlined(context, Design.SURFACE, Design.DIVIDER, 16, 1));
        int[] colors = {Design.ACCENT, Design.CYAN, 0xFFFF6B9C, 0xFFFFB454, 0xFF54E6A5};
        int selected = preferences.getInt("accent_palette", 0);
        for (int index = 0; index < colors.length; index++) {
            final int paletteIndex = index;
            FrameLayout holder = new FrameLayout(context);
            holder.setContentDescription("Accent preset " + (index + 1));
            holder.setClickable(true);
            holder.setFocusable(true);
            GradientDrawable holderBackground = new GradientDrawable();
            holderBackground.setShape(GradientDrawable.OVAL);
            holderBackground.setColor(index == selected ? 0x22FFFFFF : Color.TRANSPARENT);
            if (index == selected) {
                holderBackground.setStroke(Design.dp(context, 2), Design.TEXT);
            }
            holder.setBackground(holderBackground);

            View swatch = new View(context);
            GradientDrawable swatchBackground = new GradientDrawable();
            swatchBackground.setShape(GradientDrawable.OVAL);
            swatchBackground.setColor(colors[index]);
            swatch.setBackground(swatchBackground);
            FrameLayout.LayoutParams swatchParams = new FrameLayout.LayoutParams(
                    Design.dp(context, 24),
                    Design.dp(context, 24),
                    Gravity.CENTER
            );
            holder.addView(swatch, swatchParams);
            holder.setOnClickListener(view -> {
                preferences.putInt("accent_palette", paletteIndex);
                bridge.onValueChanged("accent_palette", paletteIndex);
                renderSelectedTab();
            });

            LayoutParams holderParams = new LayoutParams(Design.dp(context, 42), Design.dp(context, 42));
            if (index > 0) {
                holderParams.setMargins(Design.dp(context, 10), 0, 0, 0);
            }
            palette.addView(holder, holderParams);
        }
        content.addView(palette);
    }

    private void addActionButton(String text, boolean danger, Runnable action) {
        Context context = getContext();
        TextView button = Design.text(context, text, 11, danger ? Design.DANGER : Design.TEXT);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setLetterSpacing(0.09f);
        button.setGravity(Gravity.CENTER);
        button.setClickable(true);
        button.setFocusable(true);
        button.setBackground(Design.ripple(
                danger ? 0x33FF647C : 0x337C5CFF,
                danger
                        ? Design.outlined(context, 0x14FF647C, 0x66FF647C, 14, 1)
                        : Design.gradient(context, 0xFF6446E8, 0xFF2E7D91, 14)
        ));
        button.setOnClickListener(view -> action.run());
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, Design.dp(context, 46));
        params.setMargins(0, 0, 0, Design.dp(context, 8));
        content.addView(button, params);
    }

    private View createFooter(Context context) {
        LinearLayout footer = Design.horizontal(context);
        footer.setPadding(Design.dp(context, 16), 0, Design.dp(context, 16), 0);
        TextView left = Design.label(context, "Demo bridge · local only");
        left.setTextColor(Design.SUBTLE);
        footer.addView(left, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        TextView version = Design.label(context, "v1.0.0");
        version.setTextColor(Design.ACCENT_LIGHT);
        footer.addView(version);
        return footer;
    }

    private View divider(Context context) {
        View view = new View(context);
        view.setBackgroundColor(Design.DIVIDER);
        return view;
    }

    private static String formatPercent(float value) {
        return String.format(Locale.US, "%d%%", Math.round(value * 100));
    }
}
