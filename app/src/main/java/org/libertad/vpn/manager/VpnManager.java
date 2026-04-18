package org.libertad.vpn.manager;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import org.libertad.lib.v2ray.V2rayController;
import org.libertad.lib.v2ray.utils.V2rayConstants;

import java.util.ArrayList;

public class VpnManager {
    public static void toggle(Activity activity) {
        V2rayConstants.CONNECTION_STATES state =
            V2rayController.getConnectionState();

        if (state != V2rayConstants.CONNECTION_STATES.DISCONNECTED) {
            V2rayController.stopV2ray(activity);
        }
        else {
            connect(activity);
        }
    }

    public static void reconnect(Activity activity) {
        V2rayConstants.CONNECTION_STATES state = V2rayController.getConnectionState();

        if (state != V2rayConstants.CONNECTION_STATES.DISCONNECTED) {
            V2rayController.stopV2ray(activity);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                connect(activity);
            }, 800);
        }
    }

    public static void connect(Activity activity) {
        String config = activity
            .getSharedPreferences("vpn", Activity.MODE_PRIVATE)
            .getString("selected_config", null);

        if (config == null) return;

        try {
            String xray = ConfigParser.convertToXrayConfig(config);

            V2rayController.startV2ray(
                activity,
                "Libertad VPN",
                xray,
                new ArrayList<>()
            );

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}