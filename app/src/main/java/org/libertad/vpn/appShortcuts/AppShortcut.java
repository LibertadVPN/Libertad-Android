package org.libertad.vpn.appShortcuts;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;

import org.libertad.vpn.manager.VpnManager;

public class AppShortcut extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri data = getIntent().getData();

        if (data != null && "vpn://toggle".equals(data.toString())) {
            VpnManager.toggle(this);
        }

        finish();
    }
}