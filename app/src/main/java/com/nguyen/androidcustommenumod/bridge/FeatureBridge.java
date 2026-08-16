package com.nguyen.androidcustommenumod.bridge;

/**
 * Keeps the menu UI independent from app-specific behavior.
 *
 * Implement this interface only for software you own or are authorized to test.
 */
public interface FeatureBridge {
    void onToggleChanged(String featureId, boolean enabled);

    void onValueChanged(String featureId, float value);

    void onAction(String actionId);
}
