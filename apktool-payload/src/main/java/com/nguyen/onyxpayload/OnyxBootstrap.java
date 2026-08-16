package com.nguyen.onyxpayload;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

import com.nguyen.onyxmenu.overlay.MenuOverlayService;

public final class OnyxBootstrap {
    private OnyxBootstrap() {
    }

    public static void launch(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        if (Settings.canDrawOverlays(context)) {
            startEngine(context);
            return;
        }
        Intent intent = new Intent(context, OnyxPermissionActivity.class);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    static void startEngine(Context context) {
        Intent serviceIntent = new Intent(context, MenuOverlayService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
