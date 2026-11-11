package com.meraj.liveai; // अपनी पहली लाइन वाला पैकेज नाम यही रहने दें

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.method.PasswordTransformationMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText etApiKey;
    private Button btnSaveKey, btnStartService;
    private ImageButton btnToggleVisibility;
    private boolean isPasswordVisible = false;

    // Prefs की चाबियाँ
    public static final String PREFS_NAME = "MerajAiPrefs";
    public static final String KEY_API = "GEMINI_API_KEY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etApiKey = findViewById(R.id.etApiKey);
        btnSaveKey = findViewById(R.id.btnSaveKey);
        btnStartService = findViewById(R.id.btnStartService);
        btnToggleVisibility = findViewById(R.id.btnToggleVisibility);

        // पहले से सेव की गई असली Key लोड करें
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedKey = prefs.getString(KEY_API, "");
        if (!savedKey.isEmpty()) {
            etApiKey.setText(savedKey);
        }

        // पासवर्ड को डॉट (••••) में दिखाना (बिना असली टेक्स्ट खराब किए)
        etApiKey.setTransformationMethod(PasswordTransformationMethod.getInstance());

        // आँख / लॉक बटन से शो/हाइड
        if (btnToggleVisibility != null) {
            btnToggleVisibility.setOnClickListener(v -> {
                if (isPasswordVisible) {
                    etApiKey.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    isPasswordVisible = false;
                } else {
                    etApiKey.setTransformationMethod(null);
                    isPasswordVisible = true;
                }
                etApiKey.setSelection(etApiKey.getText().length());
            });
        }

        // सेव बटन का लॉजिक
        btnSaveKey.setOnClickListener(v -> {
            String rawKey = etApiKey.getText().toString().trim();
            if (rawKey.isEmpty()) {
                Toast.makeText(this, "कृपया पहले API Key डालें!", Toast.LENGTH_SHORT).show();
                return;
            }

            // असली Key सुरक्षित स्टोर होगी
            prefs.edit().putString(KEY_API, rawKey).apply();
            Toast.makeText(this, "API Key सुरक्षित रूप से सेव हो गई!", Toast.LENGTH_SHORT).show();
        });

        // परमिशन और सर्विस चालू करने का बटन
        if (btnStartService != null) {
            btnStartService.setOnClickListener(v -> checkPermissionsAndStart());
        }
    }

    private void checkPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            Toast.makeText(this, "फ्लोटिंग बटन के लिए 'Display over other apps' परमिशन दें", Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
        Toast.makeText(this, "कृपया Accessibility में जाकर Meraj Live AI चालू करें", Toast.LENGTH_LONG).show();
    }
}
