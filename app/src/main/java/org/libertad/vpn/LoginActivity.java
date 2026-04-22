package org.libertad.vpn;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Base64;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import android.net.Uri;

import org.libertad.vpn.updateChecker.UpdateChecker;

import java.io.IOException;

import okhttp3.*;

public class LoginActivity extends AppCompatActivity {
    private EditText input_token;
    private TextView info_token;
    private Button btn_login;

    private UpdateChecker updateChecker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        updateChecker = new UpdateChecker(this);
        updateChecker.check();

        setContentView(R.layout.activity_login);

        input_token = findViewById(R.id.input_token);
        btn_login = findViewById(R.id.btn_login);
        info_token = findViewById(R.id.info_token);

        info_token.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("tg://resolve?domain=LibertadManager"));
                startActivity(intent);
            }
        });

        btn_login.setOnClickListener(v -> {
            String token = input_token.getText().toString().trim();

            if (token.isEmpty()) {
                Toast.makeText(this, "Введите токен", Toast.LENGTH_SHORT).show();
                return;
            }

            login(token);
        });
    }

    private void login(String token) {
        OkHttpClient client = new OkHttpClient();

        String url = BuildConfig.subUrl + token;

        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() ->
                        Toast.makeText(LoginActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show()
                );
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                if (!response.isSuccessful()) {
                    runOnUiThread(() ->
                            Toast.makeText(LoginActivity.this, "Неверный токен", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                String base64 = response.body().string().trim();

                try {
                    byte[] decoded = Base64.decode(base64, Base64.DEFAULT);
                    String decodedStr = new String(decoded);

                    StringBuilder cleaned = new StringBuilder();
                    for (String line : decodedStr.split("\n")) {
                        line = line.trim();

                        if (!line.isEmpty()) {
                            cleaned.append(line).append("\n");
                        }
                    }

                    if (cleaned.length() == 0) {
                        throw new Exception("Нет конфигов");
                    }

                    saveAuth(token, cleaned.toString());

                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, "Успешный вход", Toast.LENGTH_SHORT).show();

                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    });

                }
                catch (Exception e) {
                    e.printStackTrace();

                    runOnUiThread(() ->
                        Toast.makeText(LoginActivity.this, "Ошибка обработки подписки", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        });
    }

    private void saveAuth(String token, String configs) {
        getSharedPreferences("vpn", MODE_PRIVATE)
            .edit()
            .putString("token", token)
            .putString("configs", configs)
            .apply();
    }
}