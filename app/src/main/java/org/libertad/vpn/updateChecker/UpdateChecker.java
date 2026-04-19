package org.libertad.vpn.updateChecker;

import android.content.Context;
import android.content.Intent;

import org.json.JSONObject;
import org.libertad.vpn.BuildConfig;

import java.io.IOException;

import okhttp3.*;

public class UpdateChecker {
    private final OkHttpClient client = new OkHttpClient();
    private final Context context;

    public UpdateChecker(Context context) {
        this.context = context.getApplicationContext();
    }

    public void check() {
        Request request = new Request.Builder()
            .url("https://api.github.com/repos/" + BuildConfig.relUrl)
            .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful() || response.body() == null) return;

                String json = response.body().string();

                try {
                    JSONObject obj = new JSONObject(json);
                    String latestVersion = obj.getString("tag_name");

                    handleVersion(latestVersion);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void handleVersion(String latestVersion) {
        try {
            String currentVersion = context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0)
                .versionName;

            currentVersion = currentVersion.replace("v", "");
            latestVersion = latestVersion.replace("v", "");

            if (isOutdated(currentVersion, latestVersion)) {
                Intent intent = new Intent(context, UpdateActivity.class);
                intent.putExtra("version", latestVersion);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                context.startActivity(intent);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isOutdated(String current, String latest) {
        String[] c = current.split("\\.");
        String[] l = latest.split("\\.");

        int len = Math.max(c.length, l.length);

        for (int i = 0; i < len; i++) {
            int cv = i < c.length ? Integer.parseInt(c[i]) : 0;
            int lv = i < l.length ? Integer.parseInt(l[i]) : 0;

            if (cv < lv) return true;
            if (cv > lv) return false;
        }

        return false;
    }
}