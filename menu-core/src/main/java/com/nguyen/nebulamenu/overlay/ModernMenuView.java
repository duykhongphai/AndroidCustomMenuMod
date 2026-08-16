package com.nguyen.nebulamenu.overlay;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.nguyen.nebulamenu.bridge.FeatureBridge;
import com.nguyen.nebulamenu.model.MenuControl;
import com.nguyen.nebulamenu.model.MenuHero;
import com.nguyen.nebulamenu.model.MenuMetric;
import com.nguyen.nebulamenu.model.MenuOption;
import com.nguyen.nebulamenu.model.MenuProfile;
import com.nguyen.nebulamenu.model.MenuSection;
import com.nguyen.nebulamenu.model.MenuTab;
import com.nguyen.nebulamenu.storage.PreferenceStore;
import com.nguyen.nebulamenu.ui.BrandMarkView;
import com.nguyen.nebulamenu.ui.Design;
import com.nguyen.nebulamenu.ui.ModernSlider;
import com.nguyen.nebulamenu.ui.ModernToggle;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@SuppressLint("ViewConstructor")
public final class ModernMenuView extends LinearLayout {
    private final MenuProfile profile;
    private final FeatureBridge bridge;
    private final PreferenceStore preferences;
    private final LinearLayout content;
    private final List<TextView> tabViews = new ArrayList<>();
    private final View dragHandle;
    private Runnable collapseListener;
    private int selectedTab;

    public ModernMenuView(
            Context context,
            MenuProfile profile,
            FeatureBridge bridge,
            PreferenceStore preferences
    ) {
        super(context);
        this.profile = profile;
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
        TextView name = Design.title(context, profile.getTitle(), 15);
        name.setLetterSpacing(0.14f);
        TextView subtitle = Design.text(context, profile.getSubtitle(), 9, Design.MUTED);
        subtitle.setLetterSpacing(0.12f);
        subtitle.setPadding(0, Design.dp(context, 4), 0, 0);
        identity.addView(name, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        identity.addView(subtitle, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        header.addView(identity, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        TextView live = Design.pill(context, "● LIVE", Design.SUCCESS, 0x1F54E6A5);
        header.addView(live, new LayoutParams(LayoutParams.WRAP_CONTENT, Design.dp(context, 28)));

        TextView minimize = Design.iconButton(context, "—", "Collapse menu");
        LayoutParams minimizeParams = new LayoutParams(Design.dp(context, 36), Design.dp(context, 36));
        minimizeParams.setMargins(Design.dp(context, 8), 0, 0, 0);
        minimize.setOnClickListener(view -> {
            if (collapseListener != null) {
                collapseListener.run();
            }
        });
        header.addView(minimize, minimizeParams);

        TextView close = Design.iconButton(context, "×", "Collapse menu");
        LayoutParams closeParams = new LayoutParams(Design.dp(context, 36), Design.dp(context, 36));
        closeParams.setMargins(Design.dp(context, 6), 0, 0, 0);
        close.setTextColor(Design.DANGER);
        close.setOnClickListener(view -> {
            if (collapseListener != null) {
                collapseListener.run();
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
        List<MenuTab> definitions = profile.getTabs();
        for (int index = 0; index < definitions.size(); index++) {
            final int tabIndex = index;
            TextView tab = Design.text(context, definitions.get(index).getLabel(), 10, Design.MUTED);
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
        MenuTab tab = profile.getTabs().get(selectedTab);
        if (tab.getHero() != null) {
            addHero(tab.getHero());
        }
        for (MenuSection section : tab.getSections()) {
            addSectionTitle(section.getTitle(), section.getMeta());
            for (MenuControl control : section.getControls()) {
                renderControl(control);
            }
        }
    }

    private void addHero(MenuHero hero) {
        Context context = getContext();
        boolean hasMetrics = !hero.getMetrics().isEmpty();
        LinearLayout card = Design.vertical(context);
        card.setPadding(
                Design.dp(context, 16),
                Design.dp(context, 15),
                Design.dp(context, 16),
                Design.dp(context, 15)
        );
        GradientDrawable background = hasMetrics
                ? Design.gradient(context, 0xFF24204B, 0xFF122C3A, 18)
                : Design.outlined(context, Design.SURFACE, 0xFF2B344A, 18, 1);
        if (hasMetrics) {
            background.setStroke(Design.dp(context, 1), 0x667C5CFF);
        }
        card.setBackground(background);

        LinearLayout top = Design.horizontal(context);
        TextView eyebrow = Design.label(context, hero.getEyebrow());
        eyebrow.setTextColor(hasMetrics ? Design.ACCENT_LIGHT : Design.CYAN);
        top.addView(eyebrow, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        if (hasMetrics) {
            top.addView(Design.pill(context, "01", Design.CYAN, 0x1F2DE2E6));
        }
        card.addView(top);

        TextView title = Design.title(context, hero.getHeading(), hasMetrics ? 21 : 20);
        title.setLineSpacing(0, 0.95f);
        LayoutParams titleParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        titleParams.setMargins(0, Design.dp(context, 11), 0, 0);
        card.addView(title, titleParams);

        TextView description = Design.text(context, hero.getDescription(), 12,
                hasMetrics ? 0xFFB9C2D5 : Design.MUTED);
        description.setLineSpacing(Design.dp(context, 3), 1f);
        LayoutParams descriptionParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        descriptionParams.setMargins(0, Design.dp(context, 9), 0, 0);
        card.addView(description, descriptionParams);
        content.addView(card, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        if (hasMetrics) {
            addMetrics(hero.getMetrics());
        }
    }

    private void addMetrics(List<MenuMetric> metrics) {
        Context context = getContext();
        LinearLayout row = Design.horizontal(context);
        LayoutParams rowParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(0, Design.dp(context, 9), 0, 0);
        content.addView(row, rowParams);
        for (int index = 0; index < metrics.size(); index++) {
            MenuMetric metric = metrics.get(index);
            LinearLayout card = Design.vertical(context);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(Design.dp(context, 14), 0, Design.dp(context, 14), 0);
            card.setBackground(Design.outlined(context, Design.SURFACE, Design.DIVIDER, 16, 1));
            TextView value = Design.title(context, metric.getValue(), 20);
            value.setTextColor(index % 2 == 0 ? Design.ACCENT_LIGHT : Design.CYAN);
            card.addView(value);
            TextView label = Design.label(context, metric.getLabel());
            LayoutParams labelParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            labelParams.setMargins(0, Design.dp(context, 5), 0, 0);
            card.addView(label, labelParams);

            LayoutParams params = new LayoutParams(0, Design.dp(context, 72), 1f);
            if (index > 0) {
                params.setMargins(Design.dp(context, 9), 0, 0, 0);
            }
            row.addView(card, params);
        }
    }

    private void renderControl(MenuControl control) {
        switch (control.getType()) {
            case TOGGLE:
                addToggleCard(control);
                break;
            case SLIDER:
                addSliderCard(control);
                break;
            case ACTION:
                addActionButton(control);
                break;
            case PALETTE:
                addPalettePicker(control);
                break;
        }
    }

    private void addSectionTitle(String title, String meta) {
        Context context = getContext();
        LinearLayout row = Design.horizontal(context);
        LayoutParams rowParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        rowParams.setMargins(Design.dp(context, 2), Design.dp(context, 20),
                Design.dp(context, 2), Design.dp(context, 8));
        content.addView(row, rowParams);
        row.addView(Design.title(context, title, 13), new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        if (!meta.isEmpty()) {
            TextView metaView = Design.label(context, meta);
            metaView.setTextColor(Design.SUBTLE);
            row.addView(metaView);
        }
    }

    private void addToggleCard(MenuControl control) {
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

        LinearLayout copy = controlCopy(control);
        row.addView(copy, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        String key = preferenceKey(control);
        ModernToggle toggle = new ModernToggle(context);
        toggle.setContentDescription(control.getTitle());
        toggle.setChecked(preferences.getBoolean(key, control.isDefaultEnabled()));
        toggle.setOnCheckedChangeListener(checked -> {
            preferences.putBoolean(key, checked);
            bridge.onToggleChanged(control.getId(), checked);
        });
        LayoutParams toggleParams = new LayoutParams(Design.dp(context, 50), Design.dp(context, 28));
        toggleParams.setMargins(Design.dp(context, 12), 0, 0, 0);
        row.addView(toggle, toggleParams);
        row.setOnClickListener(view -> toggle.performClick());
        content.addView(row, controlParams());
    }

    private void addSliderCard(MenuControl control) {
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
        heading.addView(controlCopy(control), new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        String key = preferenceKey(control);
        float initial = preferences.getFloat(key, control.getDefaultValue());
        TextView valueView = Design.pill(context, formatValue(control, initial),
                Design.CYAN, 0x1F2DE2E6);
        heading.addView(valueView);
        card.addView(heading);

        ModernSlider slider = new ModernSlider(context);
        slider.setContentDescription(control.getTitle());
        float range = control.getMaximum() - control.getMinimum();
        slider.setValue((initial - control.getMinimum()) / range);
        slider.setOnValueChangeListener((normalized, fromUser) -> {
            float actual = control.getMinimum() + normalized * range;
            valueView.setText(formatValue(control, actual));
            if (fromUser) {
                preferences.putFloat(key, actual);
                bridge.onValueChanged(control.getId(), actual);
            }
        });
        LayoutParams sliderParams = new LayoutParams(LayoutParams.MATCH_PARENT, Design.dp(context, 38));
        sliderParams.setMargins(0, Design.dp(context, 8), 0, 0);
        card.addView(slider, sliderParams);
        content.addView(card, controlParams());
    }

    private void addActionButton(MenuControl control) {
        Context context = getContext();
        boolean danger = control.isDanger();
        TextView button = Design.text(context, control.getTitle(), 11,
                danger ? Design.DANGER : Design.TEXT);
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
        button.setOnClickListener(view -> {
            bridge.onAction(control.getId());
            Toast.makeText(context, control.getTitle(), Toast.LENGTH_SHORT).show();
        });
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, Design.dp(context, 46));
        params.setMargins(0, 0, 0, Design.dp(context, 8));
        content.addView(button, params);
    }

    private void addPalettePicker(MenuControl control) {
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

        String key = preferenceKey(control);
        String selected = preferences.getString(key, control.getDefaultOptionId());
        List<MenuOption> options = control.getOptions();
        for (int index = 0; index < options.size(); index++) {
            MenuOption option = options.get(index);
            FrameLayout holder = new FrameLayout(context);
            holder.setContentDescription(option.getLabel());
            holder.setClickable(true);
            holder.setFocusable(true);
            boolean isSelected = option.getId().equals(selected);
            GradientDrawable holderBackground = new GradientDrawable();
            holderBackground.setShape(GradientDrawable.OVAL);
            holderBackground.setColor(isSelected ? 0x22FFFFFF : Color.TRANSPARENT);
            if (isSelected) {
                holderBackground.setStroke(Design.dp(context, 2), Design.TEXT);
            }
            holder.setBackground(holderBackground);

            View swatch = new View(context);
            GradientDrawable swatchBackground = new GradientDrawable();
            swatchBackground.setShape(GradientDrawable.OVAL);
            swatchBackground.setColor(option.getColor());
            swatch.setBackground(swatchBackground);
            holder.addView(swatch, new FrameLayout.LayoutParams(
                    Design.dp(context, 24),
                    Design.dp(context, 24),
                    Gravity.CENTER
            ));
            holder.setOnClickListener(view -> {
                preferences.putString(key, option.getId());
                bridge.onChoiceChanged(control.getId(), option.getId());
                renderSelectedTab();
            });

            LayoutParams holderParams = new LayoutParams(Design.dp(context, 42), Design.dp(context, 42));
            if (index > 0) {
                holderParams.setMargins(Design.dp(context, 10), 0, 0, 0);
            }
            palette.addView(holder, holderParams);
        }
        content.addView(palette, controlParams());
    }

    private LinearLayout controlCopy(MenuControl control) {
        Context context = getContext();
        LinearLayout copy = Design.vertical(context);
        copy.addView(Design.title(context, control.getTitle(), 13));
        if (!control.getDescription().isEmpty()) {
            TextView description = Design.text(context, control.getDescription(), 11, Design.MUTED);
            description.setPadding(0, Design.dp(context, 5), 0, 0);
            copy.addView(description);
        }
        return copy;
    }

    private LayoutParams controlParams() {
        LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 0, Design.dp(getContext(), 8));
        return params;
    }

    private View createFooter(Context context) {
        LinearLayout footer = Design.horizontal(context);
        footer.setPadding(Design.dp(context, 16), 0, Design.dp(context, 16), 0);
        TextView left = Design.label(context, profile.getFooter());
        left.setTextColor(Design.SUBTLE);
        footer.addView(left, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));
        TextView version = Design.label(context, profile.getVersion());
        version.setTextColor(Design.ACCENT_LIGHT);
        footer.addView(version);
        return footer;
    }

    private View divider(Context context) {
        View view = new View(context);
        view.setBackgroundColor(Design.DIVIDER);
        return view;
    }

    private String preferenceKey(MenuControl control) {
        return preferences.key(profile.getId(), control.getId());
    }

    private static String formatValue(MenuControl control, float value) {
        if (control.getMinimum() >= 0f && control.getMaximum() <= 1f) {
            return String.format(Locale.US, "%d%%", Math.round(value * 100));
        }
        return String.format(Locale.US, "%.1f", value);
    }
}
