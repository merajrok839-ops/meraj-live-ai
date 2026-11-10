package com.meraj.liveai;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("AiPrefs", MODE_PRIVATE);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 50, 40, 40);
        layout.setBackgroundColor(Color.parseColor("#F4F6F9"));

        TextView title = new TextView(this);
        title.setText("⚡ Meraj Live AI Assistant");
        title.setTextSize(22);
        title.setTextColor(Color.parseColor("#1A73E8"));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 25);
        layout.addView(title);

        TextView nameLabel = new TextView(this);
        nameLabel.setText("अपने AI असिस्टेंट का नाम रखें:");
        nameLabel.setPadding(0, 10, 0, 5);
        layout.addView(nameLabel);

        final EditText etName = new EditText(this);
        etName.setHint("उदा. Meraj AI");
        etName.setText(prefs.getString("assistant_name", "Meraj AI"));
        etName.setBackgroundColor(Color.WHITE);
        etName.setPadding(25, 25, 25, 25);
        layout.addView(etName);

        TextView keyLabel = new TextView(this);
        keyLabel.setText("\nGoogle Gemini API Key (3 फ्री ट्रायल के बाद):");
        keyLabel.setPadding(0, 10, 0, 5);
        layout.addView(keyLabel);

        final EditText etKey = new EditText(this);
        etKey.setHint("अपनी Gemini API Key यहाँ डालें");
        etKey.setText(prefs.getString("user_gemini_key", ""));
        etKey.setBackgroundColor(Color.WHITE);
        etKey.setPadding(25, 25, 25, 25);
        layout.addView(etKey);

        Button btnSave = new Button(this);
        btnSave.setText("✔ सेटिंग सुरक्षित करें (Save)");
        btnSave.setBackgroundColor(Color.parseColor("#1A73E8"));
        btnSave.setTextColor(Color.WHITE);
        btnSave.setOnClickListener(v -> {
            prefs.edit()
                .putString("assistant_name", etName.getText().toString().trim())
                .putString("user_gemini_key", etKey.getText().toString().trim())
                .apply();
            Toast.makeText(this, "सेटिंग सुरक्षित हो गई!", Toast.LENGTH_SHORT).show();
        });
        layout.addView(btnSave);

        TextView permLabel = new TextView(this);
        permLabel.setText("\nजरूरी परमिशन चालू करें:");
        permLabel.setPadding(0, 15, 0, 5);
        layout.addView(permLabel);

        Button btnOverlay = new Button(this);
        btnOverlay.setText("1. स्क्रीन पर तैरने वाला बटन Allow करें");
        btnOverlay.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))));
        layout.addView(btnOverlay);

        Button btnAccess = new Button(this);
        btnAccess.setText("2. ऑटो-क्लिक व सर्विस चालू करें");
        btnAccess.setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            Toast.makeText(this, "डाउनलोड ऐप्स में 'Meraj Live AI' ऑन करें", Toast.LENGTH_LONG).show();
        });
        layout.addView(btnAccess);

        scrollView.addView(layout);
        setContentView(scrollView);
    }
}
