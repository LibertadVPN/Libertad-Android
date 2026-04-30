package org.bobrolend.vpn.manager;

import androidx.appcompat.app.AppCompatDelegate;

import android.content.Context;
import android.content.SharedPreferences;

public class ThemeManager {
    private static final String PREFS = "vpn";
    private static final String KEY_DARK = "dark_theme";

    public static void applyTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        boolean isDark = prefs.getBoolean(KEY_DARK, false);

        AppCompatDelegate.setDefaultNightMode(
            isDark
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    public static void toggleTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        boolean isDark = prefs.getBoolean(KEY_DARK, false);
        isDark = !isDark;

        prefs.edit().putBoolean(KEY_DARK, isDark).apply();

        AppCompatDelegate.setDefaultNightMode(
            isDark
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    public static boolean isDark(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_DARK, false);
    }
}