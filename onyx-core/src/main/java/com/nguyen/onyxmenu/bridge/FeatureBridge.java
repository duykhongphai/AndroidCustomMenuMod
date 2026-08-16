package com.nguyen.onyxmenu.bridge;

/**
 * Keeps the menu UI independent from app-specific behavior.
 *
 * Implement this interface only for software you own or are authorized to test.
 */
public interface FeatureBridge {
    default void onToggleChanged(String featureId, boolean enabled) {
    }

    default void onValueChanged(String featureId, float value) {
    }

    default void onChoiceChanged(String featureId, String optionId) {
    }

    default void onAction(String actionId) {
    }
}
