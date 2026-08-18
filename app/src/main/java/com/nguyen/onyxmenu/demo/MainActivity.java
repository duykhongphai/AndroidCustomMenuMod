package com.nguyen.onyxmenu.demo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Space;
import android.widget.TextView;
import android.widget.Toast;

import com.nguyen.onyxmenu.nativebridge.NativeDemoRuntime;
import com.nguyen.onyxmenu.demo.profile.DemoFeatureBridge;
import com.nguyen.onyxmenu.overlay.MenuOverlayService;
import com.nguyen.onyxmenu.ui.BrandMarkView;
import com.nguyen.onyxmenu.ui.Design;
import com.nguyen.onyxmenu.ui.ModernSlider;
import com.nguyen.onyxmenu.ui.ModernToggle;
import com.nguyen.onyxmenu.ui.OnyxBackgroundView;

public final class MainActivity extends Activity {
    private static final int REQUEST_OVERLAY = 1001;
    private static final int REQUEST_NOTIFICATIONS = 1002;
    private TextView statusPill;
    private TextView nativeSnapshotView;
    private DemoFeatureBridge previewBridge;
    private boolean startAfterPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        previewBridge = new DemoFeatureBridge(this);
        configureWindow();
        setContentView(createContent());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        refreshNativeSnapshot();
        if (startAfterPermission && canDrawOverlays()) {
            startAfterPermission = false;
            requestNotificationsAndStart();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_NOTIFICATIONS) {
            startOverlayService();
        }
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Design.INK);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(
                        0,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
                );
            }
        } else {
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
        }
    }

    private View createContent() {
        FrameLayout root = new FrameLayout(this);
        root.addView(new OnyxBackgroundView(this), new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setClipToPadding(false);
        scrollView.setFillViewport(true);
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout content = Design.vertical(this);
        content.setPadding(
                Design.dp(this, 22),
                Design.dp(this, 58),
                Design.dp(this, 22),
                Design.dp(this, 44)
        );
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));
        root.addView(scrollView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        content.addView(createTopBar());

        TextView eyebrow = Design.label(this, "Zero-dependency overlay UI kit");
        eyebrow.setTextColor(Design.CYAN);
        LinearLayout.LayoutParams eyebrowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        eyebrowParams.setMargins(0, Design.dp(this, 52), 0, 0);
        content.addView(eyebrow, eyebrowParams);

        TextView headline = Design.title(this, "Make utility feel\nbeautiful.", 40);
        headline.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        headline.setLetterSpacing(-0.025f);
        headline.setLineSpacing(0, 0.92f);
        LinearLayout.LayoutParams headlineParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        headlineParams.setMargins(0, Design.dp(this, 15), 0, 0);
        content.addView(headline, headlineParams);

        TextView intro = Design.text(
                this,
                "Onyx is a modern floating control surface built entirely with Android Views and Canvas — portable, themeable and easy to understand.",
                15,
                0xFFB2BCD0
        );
        intro.setLineSpacing(Design.dp(this, 5), 1f);
        LinearLayout.LayoutParams introParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        introParams.setMargins(0, Design.dp(this, 19), 0, 0);
        content.addView(intro, introParams);

        LinearLayout actions = Design.horizontal(this);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Design.dp(this, 54)
        );
        actionsParams.setMargins(0, Design.dp(this, 28), 0, 0);
        content.addView(actions, actionsParams);

        TextView launch = actionButton("LAUNCH OVERLAY", false);
        launch.setOnClickListener(view -> launchOverlay());
        actions.addView(launch, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        TextView stop = actionButton("STOP", true);
        stop.setOnClickListener(view -> {
            stopService(new Intent(this, MenuOverlayService.class));
            Toast.makeText(this, "Overlay stopped", Toast.LENGTH_SHORT).show();
            updateStatus();
        });
        LinearLayout.LayoutParams stopParams = new LinearLayout.LayoutParams(
                Design.dp(this, 94),
                ViewGroup.LayoutParams.MATCH_PARENT
        );
        stopParams.setMargins(Design.dp(this, 10), 0, 0, 0);
        actions.addView(stop, stopParams);

        TextView permissionNote = Design.text(
                this,
                "Android will ask for display-over-other-apps access. No network permission is requested.",
                11,
                Design.MUTED
        );
        permissionNote.setLineSpacing(Design.dp(this, 3), 1f);
        permissionNote.setCompoundDrawablePadding(Design.dp(this, 8));
        LinearLayout.LayoutParams noteParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        noteParams.setMargins(Design.dp(this, 2), Design.dp(this, 12), 0, 0);
        content.addView(permissionNote, noteParams);

        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        previewParams.setMargins(0, Design.dp(this, 42), 0, 0);
        content.addView(createPreviewCard(), previewParams);

        LinearLayout.LayoutParams nativeStateParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        nativeStateParams.setMargins(0, Design.dp(this, 12), 0, 0);
        content.addView(createNativeStateCard(), nativeStateParams);

        TextView detailsLabel = Design.label(this, "Designed for portability");
        detailsLabel.setTextColor(Design.ACCENT_LIGHT);
        LinearLayout.LayoutParams detailsLabelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        detailsLabelParams.setMargins(0, Design.dp(this, 42), 0, 0);
        content.addView(detailsLabel, detailsLabelParams);

        TextView detailsTitle = Design.title(this, "Small surface. Serious polish.", 25);
        LinearLayout.LayoutParams detailsTitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        detailsTitleParams.setMargins(0, Design.dp(this, 10), 0, 0);
        content.addView(detailsTitle, detailsTitleParams);

        content.addView(featureCard(
                "01",
                "Pure Android Views",
                "No Compose, Material Components or third-party UI dependency."
        ), featureParams(true));
        content.addView(featureCard(
                "02",
                "Replaceable bridge",
                "UI events flow through one small interface; the demo only logs locally."
        ), featureParams(false));
        content.addView(featureCard(
                "03",
                "Persistent preferences",
                "Toggles, sliders, palette and bubble position survive restarts."
        ), featureParams(false));

        View divider = new View(this);
        divider.setBackgroundColor(Design.DIVIDER);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Design.dp(this, 1)
        );
        dividerParams.setMargins(0, Design.dp(this, 38), 0, Design.dp(this, 18));
        content.addView(divider, dividerParams);

        TextView footer = Design.text(
                this,
                "ONYX MENU  ·  DEMO BUILD 1.0.0\nFor apps you own or are authorized to test.",
                10,
                Design.SUBTLE
        );
        footer.setLetterSpacing(0.08f);
        footer.setLineSpacing(Design.dp(this, 6), 1f);
        content.addView(footer);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            root.setOnApplyWindowInsetsListener((view, insets) -> {
                int navigationBottom = insets.getInsets(WindowInsets.Type.navigationBars()).bottom;
                content.setPadding(
                        Design.dp(this, 22),
                        Design.dp(this, 58),
                        Design.dp(this, 22),
                        Design.dp(this, 44) + navigationBottom
                );
                return insets;
            });
        }
        return root;
    }

    private View createTopBar() {
        LinearLayout bar = Design.horizontal(this);
        BrandMarkView mark = new BrandMarkView(this);
        bar.addView(mark, new LinearLayout.LayoutParams(Design.dp(this, 46), Design.dp(this, 46)));

        LinearLayout identity = Design.vertical(this);
        identity.setPadding(Design.dp(this, 12), 0, 0, 0);
        TextView name = Design.title(this, "ONYX", 15);
        name.setLetterSpacing(0.16f);
        TextView edition = Design.text(this, "OVERLAY UI KIT", 9, Design.MUTED);
        edition.setLetterSpacing(0.13f);
        edition.setPadding(0, Design.dp(this, 5), 0, 0);
        identity.addView(name);
        identity.addView(edition);
        bar.addView(identity, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        statusPill = Design.pill(this, "CHECKING", Design.MUTED, 0x1FFFFFFF);
        bar.addView(statusPill, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Design.dp(this, 30)
        ));
        return bar;
    }

    private View createPreviewCard() {
        LinearLayout shell = Design.vertical(this);
        shell.setPadding(
                Design.dp(this, 15),
                Design.dp(this, 15),
                Design.dp(this, 15),
                Design.dp(this, 15)
        );
        shell.setBackground(Design.outlined(this, 0xE6111622, 0xFF333C52, 24, 1));
        shell.setElevation(Design.dp(this, 16));

        LinearLayout header = Design.horizontal(this);
        BrandMarkView mark = new BrandMarkView(this);
        header.addView(mark, new LinearLayout.LayoutParams(Design.dp(this, 38), Design.dp(this, 38)));
        LinearLayout nameStack = Design.vertical(this);
        nameStack.setPadding(Design.dp(this, 10), 0, 0, 0);
        nameStack.addView(Design.title(this, "CONTROL PREVIEW", 12));
        TextView subtitle = Design.label(this, "Live component sample");
        subtitle.setPadding(0, Design.dp(this, 5), 0, 0);
        nameStack.addView(subtitle);
        header.addView(nameStack, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(Design.pill(this, "● LIVE", Design.SUCCESS, 0x1F54E6A5));
        shell.addView(header);

        LinearLayout feature = Design.horizontal(this);
        feature.setPadding(
                Design.dp(this, 14),
                Design.dp(this, 13),
                Design.dp(this, 14),
                Design.dp(this, 13)
        );
        feature.setBackground(Design.outlined(this, Design.SURFACE, Design.DIVIDER, 16, 1));
        LinearLayout copy = Design.vertical(this);
        copy.addView(Design.title(this, "Ambient interface", 13));
        TextView description = Design.text(this, "Custom controls, consistent language", 11, Design.MUTED);
        description.setPadding(0, Design.dp(this, 5), 0, 0);
        copy.addView(description);
        feature.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        ModernToggle toggle = new ModernToggle(this);
        toggle.setChecked(true);
        toggle.setOnCheckedChangeListener(checked -> {
            previewBridge.onToggleChanged("preview_ambient", checked);
            refreshNativeSnapshot();
        });
        feature.addView(toggle, new LinearLayout.LayoutParams(Design.dp(this, 50), Design.dp(this, 28)));
        LinearLayout.LayoutParams featureParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        featureParams.setMargins(0, Design.dp(this, 16), 0, 0);
        shell.addView(feature, featureParams);

        LinearLayout sliderCard = Design.vertical(this);
        sliderCard.setPadding(
                Design.dp(this, 14),
                Design.dp(this, 13),
                Design.dp(this, 14),
                Design.dp(this, 8)
        );
        sliderCard.setBackground(Design.outlined(this, Design.SURFACE, Design.DIVIDER, 16, 1));
        LinearLayout sliderHeader = Design.horizontal(this);
        sliderHeader.addView(Design.title(this, "Interface intensity", 13),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView sliderValue = Design.pill(this, "68%", Design.CYAN, 0x1F2DE2E6);
        sliderHeader.addView(sliderValue);
        sliderCard.addView(sliderHeader);
        ModernSlider slider = new ModernSlider(this);
        slider.setValue(0.68f);
        slider.setOnValueChangeListener((value, fromUser) -> {
            sliderValue.setText(getString(R.string.percentage_format, Math.round(value * 100)));
            if (fromUser) {
                previewBridge.onValueChanged("preview_intensity", value);
                refreshNativeSnapshot();
            }
        });
        sliderCard.addView(slider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Design.dp(this, 42)
        ));
        LinearLayout.LayoutParams sliderCardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        sliderCardParams.setMargins(0, Design.dp(this, 9), 0, 0);
        shell.addView(sliderCard, sliderCardParams);

        return shell;
    }

    private View createNativeStateCard() {
        LinearLayout card = Design.vertical(this);
        card.setPadding(
                Design.dp(this, 15),
                Design.dp(this, 15),
                Design.dp(this, 15),
                Design.dp(this, 15)
        );
        card.setBackground(Design.outlined(this, 0xE6111622, 0xFF333C52, 20, 1));

        LinearLayout titleRow = Design.horizontal(this);
        titleRow.addView(Design.title(this, "JNI → C++ → NATIVE STATE", 13),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        titleRow.addView(Design.pill(this, "PROCESS LOCAL", Design.CYAN, 0x1F2DE2E6));
        card.addView(titleRow);

        TextView explanation = Design.text(
                this,
                "The controls above call DemoFeatureBridge, cross JNI and update a mutex-protected C++ object.",
                11,
                Design.MUTED
        );
        explanation.setLineSpacing(Design.dp(this, 3), 1f);
        LinearLayout.LayoutParams explanationParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        explanationParams.setMargins(0, Design.dp(this, 9), 0, 0);
        card.addView(explanation, explanationParams);

        nativeSnapshotView = Design.text(this, NativeDemoRuntime.snapshot(), 11, Design.ACCENT_LIGHT);
        nativeSnapshotView.setTypeface(Typeface.MONOSPACE);
        nativeSnapshotView.setTextIsSelectable(true);
        nativeSnapshotView.setPadding(
                Design.dp(this, 12),
                Design.dp(this, 11),
                Design.dp(this, 12),
                Design.dp(this, 11)
        );
        nativeSnapshotView.setBackground(Design.outlined(
                this,
                0xFF090D16,
                Design.DIVIDER,
                12,
                1
        ));
        LinearLayout.LayoutParams snapshotParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        snapshotParams.setMargins(0, Design.dp(this, 12), 0, 0);
        card.addView(nativeSnapshotView, snapshotParams);

        LinearLayout actions = Design.horizontal(this);
        TextView refresh = actionButton("REFRESH SNAPSHOT", true);
        refresh.setOnClickListener(view -> refreshNativeSnapshot());
        actions.addView(refresh, new LinearLayout.LayoutParams(
                0,
                Design.dp(this, 42),
                1f
        ));

        TextView clear = actionButton("CLEAR C++ STATE", true);
        clear.setOnClickListener(view -> {
            previewBridge.onAction("clear_demo_state");
            refreshNativeSnapshot();
        });
        LinearLayout.LayoutParams clearParams = new LinearLayout.LayoutParams(
                0,
                Design.dp(this, 42),
                1f
        );
        clearParams.setMargins(Design.dp(this, 8), 0, 0, 0);
        actions.addView(clear, clearParams);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        actionParams.setMargins(0, Design.dp(this, 10), 0, 0);
        card.addView(actions, actionParams);

        TextView offsetLab = actionButton("RUN SAFE C++ OFFSET LAB", false);
        offsetLab.setOnClickListener(view -> {
            String report = NativeDemoRuntime.runOwnedOffsetLab();
            nativeSnapshotView.setText(report);
            Toast.makeText(this, "C++ helper checks completed", Toast.LENGTH_SHORT).show();
        });
        LinearLayout.LayoutParams offsetLabParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                Design.dp(this, 44)
        );
        offsetLabParams.setMargins(0, Design.dp(this, 8), 0, 0);
        card.addView(offsetLab, offsetLabParams);
        return card;
    }

    private void refreshNativeSnapshot() {
        if (nativeSnapshotView != null) {
            nativeSnapshotView.setText(NativeDemoRuntime.snapshot());
        }
    }

    private View featureCard(String index, String title, String description) {
        LinearLayout card = Design.horizontal(this);
        card.setGravity(Gravity.TOP);
        card.setPadding(
                Design.dp(this, 16),
                Design.dp(this, 17),
                Design.dp(this, 16),
                Design.dp(this, 17)
        );
        card.setBackground(Design.outlined(this, 0xB3101522, Design.DIVIDER, 18, 1));
        TextView number = Design.pill(this, index, Design.CYAN, 0x1F2DE2E6);
        card.addView(number, new LinearLayout.LayoutParams(Design.dp(this, 42), Design.dp(this, 31)));
        LinearLayout copy = Design.vertical(this);
        copy.setPadding(Design.dp(this, 14), 0, 0, 0);
        copy.addView(Design.title(this, title, 15));
        TextView body = Design.text(this, description, 12, Design.MUTED);
        body.setLineSpacing(Design.dp(this, 3), 1f);
        body.setPadding(0, Design.dp(this, 7), 0, 0);
        copy.addView(body);
        card.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        return card;
    }

    private LinearLayout.LayoutParams featureParams(boolean first) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(0, Design.dp(this, first ? 22 : 10), 0, 0);
        return params;
    }

    private TextView actionButton(String text, boolean secondary) {
        TextView button = Design.text(this, text, 11, secondary ? Design.MUTED : Design.TEXT);
        button.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        button.setLetterSpacing(0.09f);
        button.setGravity(Gravity.CENTER);
        button.setClickable(true);
        button.setFocusable(true);
        button.setBackground(Design.ripple(
                secondary ? 0x22FFFFFF : 0x337C5CFF,
                secondary
                        ? Design.outlined(this, Design.SURFACE, Design.DIVIDER, 16, 1)
                        : Design.gradient(this, 0xFF6D4CEE, 0xFF267E91, 16)
        ));
        return button;
    }

    private void launchOverlay() {
        if (!canDrawOverlays()) {
            startAfterPermission = true;
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
            );
            startActivityForResult(intent, REQUEST_OVERLAY);
            return;
        }
        requestNotificationsAndStart();
    }

    private void requestNotificationsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    REQUEST_NOTIFICATIONS
            );
            return;
        }
        startOverlayService();
    }

    private void startOverlayService() {
        Intent intent = new Intent(this, MenuOverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        Toast.makeText(this, "Onyx overlay launched", Toast.LENGTH_SHORT).show();
        updateStatus();
    }

    private boolean canDrawOverlays() {
        return Settings.canDrawOverlays(this);
    }

    private void updateStatus() {
        if (statusPill == null) {
            return;
        }
        boolean permission = canDrawOverlays();
        boolean running = permission && isOverlayServiceRunning();
        if (running) {
            statusPill.setText(R.string.status_active);
            statusPill.setTextColor(Design.SUCCESS);
            statusPill.setBackground(Design.rounded(this, 0x1F54E6A5, 99));
        } else if (permission) {
            statusPill.setText(R.string.status_ready);
            statusPill.setTextColor(Design.CYAN);
            statusPill.setBackground(Design.rounded(this, 0x1F2DE2E6, 99));
        } else {
            statusPill.setText(R.string.status_access_needed);
            statusPill.setTextColor(Design.ACCENT_LIGHT);
            statusPill.setBackground(Design.rounded(this, 0x247C5CFF, 99));
        }
    }

    private boolean isOverlayServiceRunning() {
        return MenuOverlayService.isRunning();
    }
}
