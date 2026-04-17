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

import java.io.IOException;
import java.net.URLDecoder;
import java.util.*;

import okhttp3.*;

public class MainActivity extends AppCompatActivity {
    private Button connection;
    private TextView connection_time, txtSelected;
    private BroadcastReceiver v2rayBroadCastReceiver;
    private String selectedConfig;

    private ListView listView;
    private List<String> rawConfigs = new ArrayList<>();
    private List<String> displayNames = new ArrayList<>();

    private Handler handler = new Handler(Looper.getMainLooper());

    @SuppressLint({"SetTextI18n", "UnspecifiedRegisterReceiverFlag"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isAuthorized()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        V2rayController.init(this, R.drawable.ic_launcher, "Libertad VPN");

        connection = findViewById(R.id.btn_connection);
        connection_time = findViewById(R.id.connection_duration);
        txtSelected = findViewById(R.id.txt_selected);
        listView = findViewById(R.id.list_servers);

        loadConfigsToUI();
        startAutoUpdate();
        updateUI(V2rayController.getConnectionState());

        connection.setOnClickListener(view -> {
            V2rayConstants.CONNECTION_STATES state = V2rayController.getConnectionState();

            if (state != V2rayConstants.CONNECTION_STATES.DISCONNECTED) {
                V2rayController.stopV2ray(this);
                return;
            }

            if (selectedConfig == null) {
                Toast.makeText(this, "Выберите сервер", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                String config = convertToXrayConfig(selectedConfig);
                V2rayController.startV2ray(this, "Libertad VPN", config, null);
            }
            catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Ошибка конфига", Toast.LENGTH_SHORT).show();
            }
        });

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(
                v2rayBroadCastReceiver,
                new IntentFilter(V2RAY_SERVICE_STATICS_BROADCAST_INTENT),
                Context.RECEIVER_NOT_EXPORTED
            );
        }
        else {
            registerReceiver(
                v2rayBroadCastReceiver,
                new IntentFilter(V2RAY_SERVICE_STATICS_BROADCAST_INTENT)
            );
        }
    }

    private boolean isAuthorized() {
        return getSharedPreferences("vpn", MODE_PRIVATE)
            .getString("token", null) != null;
    }

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

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.list_item,
                R.id.txt_server,
                displayNames
        );

        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            selectedConfig = rawConfigs.get(position);

            txtSelected.setText("Выбран: " + displayNames.get(position));

            getSharedPreferences("vpn", MODE_PRIVATE)
                    .edit()
                    .putString("selected_config", selectedConfig)
                    .apply();

            adapter.notifyDataSetChanged();

            V2rayConstants.CONNECTION_STATES state = V2rayController.getConnectionState();

            try {
                String config = convertToXrayConfig(selectedConfig);

                if (state != V2rayConstants.CONNECTION_STATES.DISCONNECTED) {
                    V2rayController.stopV2ray(this);

                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        V2rayController.startV2ray(this, "Libertad VPN", config, null);
                    }, 500);
                } else {
                    V2rayController.startV2ray(this, "Libertad VPN", config, null);
                }

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Ошибка конфига", Toast.LENGTH_SHORT).show();
            }
        });

        String savedConfig = getSharedPreferences("vpn", MODE_PRIVATE)
                .getString("selected_config", null);

        selectedConfig = savedConfig;

        if (selectedConfig != null) {
            int index = rawConfigs.indexOf(selectedConfig);

            if (index != -1) {
                txtSelected.setText("Выбран: " + displayNames.get(index));
            }
        }
    }

    private String parseName(String link) {
        try {
            if (link.contains("#")) {
                String name = link.split("#")[1];
                return URLDecoder.decode(name, "UTF-8");
            }
        } catch (Exception ignored) {}
        return "Server";
    }

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

                } catch (Exception ignored) {}
            }
        });
    }

    private String convertToXrayConfig(String link) throws Exception {
        String cleanLink = link.split("#")[0];
        String raw = cleanLink.replace("vless://", "");

        String[] parts = raw.split("@");
        String uuid = parts[0];

        String[] hostAndParams = parts[1].split("\\?");
        String hostPort = hostAndParams[0];

        String[] hp = hostPort.split(":");
        String address = hp[0];
        int port = Integer.parseInt(hp[1]);

        // Defaults
        String network = "tcp";
        String security = "none";

        String path = "/";
        String host = "";

        // TLS
        String sni = "";

        // Reality
        String fp = "chrome";
        String pbk = "";
        String sid = "";
        String flow = "";
        String spx = "/";

        if (hostAndParams.length > 1) {
            String params = hostAndParams[1];

            for (String param : params.split("&")) {
                String[] kv = param.split("=");
                if (kv.length != 2) continue;

                String key = kv[0];
                String value = java.net.URLDecoder.decode(kv[1], "UTF-8");

                switch (key) {
                    // Default
                    case "type":
                        network = value;
                        break;

                    case "security":
                        security = value;
                        break;

                    case "path":
                        path = value;
                        break;

                    case "host":
                        host = value;
                        break;

                    // TLS / Reality SNI
                    case "sni":
                        sni = value;
                        break;

                    // Reality params
                    case "fp":
                        fp = value;
                        break;

                    case "pbk":
                        pbk = value;
                        break;

                    case "sid":
                        sid = value;
                        break;

                    case "flow":
                        flow = value;
                        break;

                    case "spx":
                        spx = value;
                        break;
                }
            }
        }

        // Validation

        if ("reality".equals(security)) {
            if (pbk.isEmpty() || sid.isEmpty() || sni.isEmpty()) {
                throw new Exception("Invalid Reality config: missing pbk/sid/sni");
            }
        }

        // JSON Build

        StringBuilder json = new StringBuilder();

        json.append("{\n")
            .append("  \"inbounds\": [{")
            .append("\"port\":10808,")
            .append("\"protocol\":\"socks\",")
            .append("\"settings\":{\"auth\":\"noauth\",\"udp\":true}")
            .append("}],\n")

            .append("  \"outbounds\": [{\n")
            .append("    \"protocol\":\"vless\",\n")

            .append("    \"settings\": {\n")
            .append("      \"vnext\": [{\n")
            .append("        \"address\": \"").append(address).append("\",\n")
            .append("        \"port\": ").append(port).append(",\n")
            .append("        \"users\": [{\n")
            .append("          \"id\": \"").append(uuid).append("\",\n")
            .append("          \"encryption\": \"none\"");

        if (!flow.isEmpty()) {
            json.append(",\n          \"flow\": \"").append(flow).append("\"");
        }

        json.append("\n        }]\n")
            .append("      }]\n")
            .append("    },\n")

            .append("    \"streamSettings\": {\n")
            .append("      \"network\": \"").append(network).append("\",\n")
            .append("      \"security\": \"").append(security).append("\"");

        // TLS
        if ("tls".equals(security)) {
            json.append(",\n      \"tlsSettings\": {\n")
                .append("        \"serverName\": \"").append(host).append("\"\n")
                .append("      }");
        }

        // Reality
        if ("reality".equals(security)) {
            json.append(",\n      \"realitySettings\": {\n")
                .append("        \"serverName\": \"").append(sni).append("\",\n")
                .append("        \"publicKey\": \"").append(pbk).append("\",\n")
                .append("        \"shortId\": \"").append(sid).append("\",\n")
                .append("        \"fingerprint\": \"").append(fp).append("\",\n")
                .append("        \"spiderX\": \"").append(spx).append("\"\n")
                .append("      }");
        }

        // WS
        if ("ws".equals(network)) {
            json.append(",\n      \"wsSettings\": {\n")
                .append("        \"path\": \"").append(path).append("\",\n")
                .append("        \"headers\": {\n")
                .append("          \"Host\": \"").append(host).append("\"\n")
                .append("        }\n")
                .append("      }");
        }

        json.append("\n    }\n")
            .append("  }]\n")
            .append("}");

        return json.toString();
    }

    private void updateUI(V2rayConstants.CONNECTION_STATES state) {
       switch (state) {
           case CONNECTED:
               connection.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#22C55E")));
               break;
           case DISCONNECTED:
               connection.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EF4444")));
               connection_time.setText("00:00:00");
               break;
           case CONNECTING:
               connection.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#3B82F6")));
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