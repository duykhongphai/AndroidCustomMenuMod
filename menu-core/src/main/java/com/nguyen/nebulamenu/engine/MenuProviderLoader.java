package com.nguyen.nebulamenu.engine;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

public final class MenuProviderLoader {
    public static final String META_DATA_PROVIDER = "com.nguyen.nebulamenu.MENU_PROVIDER";
    private static final String TAG = "NebulaMenuEngine";

    private MenuProviderLoader() {
    }

    public static LoadedMenu load(Context context) {
        MenuProvider provider = instantiateConfiguredProvider(context);
        if (provider != null) {
            try {
                return createLoadedMenu(context, provider);
            } catch (RuntimeException error) {
                Log.e(TAG, "Configured MenuProvider failed; using fallback profile", error);
            }
        }
        return createLoadedMenu(context, new BuiltInMenuProvider());
    }

    private static LoadedMenu createLoadedMenu(Context context, MenuProvider provider) {
        return new LoadedMenu(
                provider.createProfile(context),
                provider.createBridge(context)
        );
    }

    private static MenuProvider instantiateConfiguredProvider(Context context) {
        try {
            ApplicationInfo applicationInfo = getApplicationInfo(context);
            Bundle metadata = applicationInfo.metaData;
            String className = metadata == null ? null : metadata.getString(META_DATA_PROVIDER);
            if (className == null || className.trim().isEmpty()) {
                Log.w(TAG, "No MenuProvider metadata found; using fallback profile");
                return null;
            }
            Object instance = Class.forName(className).getDeclaredConstructor().newInstance();
            if (!(instance instanceof MenuProvider)) {
                throw new IllegalArgumentException(className + " does not implement MenuProvider");
            }
            return (MenuProvider) instance;
        } catch (ReflectiveOperationException | PackageManager.NameNotFoundException
                 | IllegalArgumentException error) {
            Log.e(TAG, "Unable to load configured MenuProvider", error);
            return null;
        }
    }

    @SuppressWarnings("deprecation")
    private static ApplicationInfo getApplicationInfo(Context context)
            throws PackageManager.NameNotFoundException {
        PackageManager manager = context.getPackageManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return manager.getApplicationInfo(
                    context.getPackageName(),
                    PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA)
            );
        }
        return manager.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
    }
}
