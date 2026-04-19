package org.libertad.vpn.manager;

import android.content.Context;
import android.os.*;

public class VibrationManager {
    public static void vibrate(Context context, int duration) {
        if (context == null) return;

        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        if (vibrator == null || !vibrator.hasVibrator()) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE));
        }
        else {
            vibrator.vibrate(duration);
        }
    }

    public static void vibratePattern(Context context, long[] pattern) {
        if (context == null) return;

        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        if (vibrator == null || !vibrator.hasVibrator()) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1));
        }
        else {
            vibrator.vibrate(pattern, -1);
        }
    }
}