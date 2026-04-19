package org.libertad.vpn;

import static org.libertad.lib.v2ray.utils.V2rayConstants.SERVICE_CONNECTION_STATE_BROADCAST_EXTRA;
import static org.libertad.lib.v2ray.utils.V2rayConstants.SERVICE_DURATION_BROADCAST_EXTRA;
import static org.libertad.lib.v2ray.utils.V2rayConstants.V2RAY_SERVICE_STATICS_BROADCAST_INTENT;

import androidx.appcompat.app.AppCompatActivity;
import android.util.Base64;
import android.annotation.SuppressLint;
import android.content.*;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.*;
import android.widget.*;

import org.libertad.lib.v2ray.V2rayController;
import org.libertad.lib.v2ray.utils.V2rayConstants;
import org.libertad.vpn.manager.VpnManager;
import org.libertad.vpn.theme.ThemeManager;
import org.libertad.vpn.updateChecker.UpdateChecker;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.*;

import okhttp3.*;

public class MainActivity extends AppCompatActivity {
    private Button connection;
    private TextView connection_time;
    private ImageButton btnTheme;
    private BroadcastReceiver v2rayBroadCastReceiver;
    private String selectedConfig;

    private ListView listView;
    private List<String> rawConfigs = new ArrayList<>();
    private List<String> displayNames = new ArrayList<>();

    private Handler handler = new Handler(Looper.getMainLooper());
    private UpdateChecker updateChecker;

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isAuthorized()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        updateChecker = new UpdateChecker(this);
        updateChecker.check();

        ThemeManager.applyTheme(this);

        setContentView(R.layout.activity_main);

        V2rayController.init(this, R.drawable.ic_launcher, "Libertad VPN");

        connection = findViewById(R.id.btn_connection);
        connection_time = findViewById(R.id.connection_duration);
        listView = findViewById(R.id.list_servers);
        btnTheme = findViewById(R.id.btn_theme);

        updateThemeIcon();

        btnTheme.setOnClickListener(v -> {
            ThemeManager.toggleTheme(this);
            updateThemeIcon();
            recreate();
        });

        connection.setOnClickListener(v -> {
            VpnManager.toggle(this);
        });

        loadConfigsToUI();
        startAutoUpdate();
        updateUI(V2rayController.getConnectionState());

        v2rayBroadCastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getExtras() == null) return;

                runOnUiThread(() -> {
                    connection_time.setText(
                        intent.getExtras().getString(SERVICE_DURATION_BROADCAST_EXTRA)
                    );

                    V2rayConstants.CONNECTION_STATES state;

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        state = intent.getSerializableExtra(
                            SERVICE_CONNECTION_STATE_BROADCAST_EXTRA,
                            V2rayConstants.CONNECTION_STATES.class
                        );
                    }
                    else {
                        state = (V2rayConstants.CONNECTION_STATES)
                            intent.getSerializableExtra(SERVICE_CONNECTION_STATE_BROADCAST_EXTRA);
                    }

                    if (state != null) {
                        updateUI(state);
                    }
                });
            }
        };

        IntentFilter filter = new IntentFilter(V2RAY_SERVICE_STATICS_BROADCAST_INTENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                v2rayBroadCastReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            );
        }
        else {
            registerReceiver(
                v2rayBroadCastReceiver,
                filter
            );
        }
    }

    // Auth
    private boolean isAuthorized() {
        return getSharedPreferences("vpn", MODE_PRIVATE)
            .getString("token", null) != null;
    }

    // Theme
    private void updateThemeIcon() {
        boolean dark = ThemeManager.isDark(this);

        btnTheme.setImageResource(
            dark ? R.drawable.ic_moon : R.drawable.ic_sun
        );
    }

    // Configs
    private void loadConfigsToUI() {
        rawConfigs.clear();
        displayNames.clear();

        String all = getSharedPreferences("vpn", MODE_PRIVATE)
            .getString("configs", "");

        if (all == null || all.isEmpty()) return;

        for (String line : all.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) continue;

            rawConfigs.add(line);
            displayNames.add(parseName(line));
        }

        String savedConfig = getSharedPreferences("vpn", MODE_PRIVATE)
            .getString("selected_config", null);

        selectedConfig = savedConfig;

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            R.layout.list_item,
            R.id.txt_server,
            displayNames
        );

        listView.setAdapter(adapter);

        if (savedConfig != null) {
            int index = rawConfigs.indexOf(savedConfig);

            if (index != -1) {
                listView.setItemChecked(index, true);
                listView.setSelection(index);
            }
        }

        listView.setOnItemClickListener((parent, view, position, id) -> {
            selectedConfig = rawConfigs.get(position);

            getSharedPreferences("vpn", MODE_PRIVATE)
                .edit()
                .putString("selected_config", selectedConfig)
                .apply();

            adapter.notifyDataSetChanged();

            try {
                VpnManager.reconnect(this);
            }
            catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Ошибка конфига", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String parseName(String link) {
        try {
            if (link.contains("#")) {
                return URLDecoder.decode(link.split("#")[1], "UTF-8");
            }
        }
        catch (Exception ignored) {}

        return "Server";
    }

    // Update
    private void startAutoUpdate() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                updateSubscription();
                handler.postDelayed(this, 3600 * 1000); // 1 час
            }
        }, 3600 * 1000);
    }

    private void updateSubscription() {
        String token = getSharedPreferences("vpn", MODE_PRIVATE)
            .getString("token", "");

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
            .url(BuildConfig.subUrl + token)
            .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {}

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) return;

                String base64 = response.body().string().trim();

                try {
                    byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
                    String decodedStr = new String(decoded);

                    getSharedPreferences("vpn", MODE_PRIVATE)
                        .edit()
                        .putString("configs", decodedStr)
                        .apply();

                    runOnUiThread(() -> loadConfigsToUI());

                }
                catch (Exception ignored) {}
            }
        });
    }

    // UI
    private void updateUI(V2rayConstants.CONNECTION_STATES state) {
        switch (state) {
            case CONNECTED:
                connection.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#22C55E"))
                );
                break;
            case DISCONNECTED:
                connection.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#EF4444"))
                );
                connection_time.setText("00:00:00");
                break;
            case CONNECTING:
                connection.setBackgroundTintList(
                    ColorStateList.valueOf(Color.parseColor("#3B82F6"))
                );
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (v2rayBroadCastReceiver != null) {
            unregisterReceiver(v2rayBroadCastReceiver);
        }
    }
}