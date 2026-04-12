package org.libertad.lib.v2ray.services;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE;
import static org.libertad.lib.v2ray.utils.V2rayConstants.V2RAY_SERVICE_OPENED_APPLICATION_INTENT;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.util.Objects;

import org.libertad.lib.v2ray.interfaces.TrafficListener;
import org.libertad.lib.v2ray.utils.V2rayConstants;
import org.libertad.lib.v2ray.utils.Utilities;

public class NotificationService {
    private NotificationManager mNotificationManager = null;
    private NotificationCompat.Builder notifcationBuilder;
    public boolean isNotificationOnGoing;
    private final int NOTIFICATION_ID = 1;

    public TrafficListener trafficListener = new TrafficListener() {
        @Override
        public void onTrafficChanged(long uploadSpeed, long downloadSpeed, long uploadedTraffic, long downloadedTraffic) {
            if (mNotificationManager != null && notifcationBuilder != null) {
                if (isNotificationOnGoing) {
                    notifcationBuilder.setSubText("Traffic ↓" + Utilities.parseTraffic(downloadedTraffic, false, false) + "  ↑" + Utilities.parseTraffic(uploadedTraffic, false, false));
                    notifcationBuilder.setContentText("Tap to open application.\n Download : ↓" + Utilities.parseTraffic(downloadSpeed, false, true) + " | Upload : ↑" + Utilities.parseTraffic(uploadSpeed, false, true));
                    mNotificationManager.notify(NOTIFICATION_ID, notifcationBuilder.build());
                }
            }
        }
    };

    public NotificationService(final Service service) {
        String channelId = "LIBERTAD_VPN";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "Libertad VPN",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager =
                    (NotificationManager) service.getSystemService(Context.NOTIFICATION_SERVICE);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        Notification notification = new NotificationCompat.Builder(service, channelId)
                .setContentTitle("Libertad VPN")
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            service.startForeground(1, notification, FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            service.startForeground(1, notification);
        }
    }

    public void setConnectedNotification(String remark, int iconResource) {
        if (mNotificationManager != null && notifcationBuilder != null) {
            if (isNotificationOnGoing) {
                notifcationBuilder.setSmallIcon(iconResource);
                notifcationBuilder.setContentTitle(remark);
                notifcationBuilder.setContentText("");
                mNotificationManager.notify(NOTIFICATION_ID, notifcationBuilder.build());
            }
        }
    }

    public void dismissNotification() {
        if (mNotificationManager != null) {
            isNotificationOnGoing = false;
            mNotificationManager.cancel(NOTIFICATION_ID);
        }
    }
}