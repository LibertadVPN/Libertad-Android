package org.libertad.vpn.updateChecker;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import org.libertad.vpn.BuildConfig;
import org.libertad.vpn.R;

public class UpdateActivity extends AppCompatActivity {
    private TextView text_version;
    private Button btn_update;

    private static final String RELEASES_URL = "https://github.com/" + BuildConfig.relUrl + "/download/LibertadVPN.apk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_update);

        text_version = findViewById(R.id.text_version);
        btn_update = findViewById(R.id.btn_update);

        String version = getIntent().getStringExtra("version");

        if (version == null) {
            version = "unknown";
        }

        text_version.setText("Доступно обновление: " + version);

        btn_update.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(RELEASES_URL));
            startActivity(intent);
        });
    }

    @Override
    public void onBackPressed() {}
}