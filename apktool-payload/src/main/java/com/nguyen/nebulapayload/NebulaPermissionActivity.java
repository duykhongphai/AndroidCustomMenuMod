package com.nguyen.nebulapayload;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

public final class NebulaPermissionActivity extends Activity {
    private boolean settingsOpened;
    private boolean leftForSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            finish();
            return;
        }
        if (Settings.canDrawOverlays(this)) {
            NebulaBootstrap.startEngine(this);
            finish();
            return;
        }
        settingsOpened = true;
        startActivity(new Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())
        ));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!settingsOpened || !leftForSettings || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        if (Settings.canDrawOverlays(this)) {
            NebulaBootstrap.startEngine(this);
        }
        finish();
    }

    @Override
    protected void onPause() {
        if (settingsOpened) {
            leftForSettings = true;
        }
        super.onPause();
    }
}
