package com.nguyen.nebulamenu.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;

import com.nguyen.nebulamenu.R;
import com.nguyen.nebulamenu.engine.LoadedMenu;
import com.nguyen.nebulamenu.engine.MenuProviderLoader;

public final class MenuOverlayService extends Service {
    private static final String CHANNEL_ID = "nebula_overlay_channel";
    private static final int NOTIFICATION_ID = 4201;
    private static volatile boolean running;
    private MenuOverlayController controller;

    public static boolean isRunning() {
        return running;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }
        LoadedMenu loadedMenu = MenuProviderLoader.load(this);
        controller = new MenuOverlayController(
                this,
                loadedMenu.getProfile(),
                loadedMenu.getBridge()
        );
        controller.show();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        if (controller != null) {
            controller.destroy();
            controller = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.nebula_overlay_channel_name),
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription(getString(R.string.nebula_overlay_channel_description));
        channel.setShowBadge(false);
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(channel);
    }

    @SuppressWarnings("deprecation")
    private Notification createNotification() {
        Intent openIntent = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (openIntent == null) {
            openIntent = new Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName())
            );
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder
                .setSmallIcon(R.drawable.nebula_ic_notification)
                .setContentTitle(getString(R.string.nebula_overlay_notification_title))
                .setContentText(getString(R.string.nebula_overlay_notification_text))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
    }
}
