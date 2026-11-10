package com.meraj.liveai;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
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
        prefs = getSharedPreferences("AIPrefs", MODE_PRIVATE);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 50, 40, 50);
        layout.setBackgroundColor(Color.parseColor("#F8F9FA"));

        TextView title = new TextView(this);
        title.setText("⚡ Meraj Live AI Assistant");
        title.setTextSize(22);
        title.setTextColor(Color.parseColor("#1A73E8"));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 25);
        layout.addView(title);

        TextView nameLabel = new TextView(this);
        nameLabel.setText("अपने AI असिस्टेंट का नाम:");
        nameLabel.setPadding(0, 10, 0, 5);
        layout.addView(nameLabel);

        final EditText etName = new EditText(this);
        etName.setText(prefs.getString("assistant_name", "Meraj AI"));
        etName.setBackgroundColor(Color.WHITE);
        etName.setPadding(25, 20, 25, 20);
        layout.addView(etName);

        final LinearLayout keyContainer = new LinearLayout(this);
        keyContainer.setOrientation(LinearLayout.VERTICAL);
        keyContainer.setPadding(0, 20, 0, 10);

        final TextView keyLabel = new TextView(this);
        keyLabel.setText("Google Gemini API Key:");
        keyLabel.setPadding(0, 0, 0, 5);
        keyContainer.addView(keyLabel);

        final EditText etKey = new EditText(this);
        etKey.setHint("यहाँ अपनी API Key पेस्ट करें");
        etKey.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etKey.setBackgroundColor(Color.WHITE);
        etKey.setPadding(25, 20, 25, 20);
        keyContainer.addView(etKey);

        layout.addView(keyContainer);

        final TextView savedStatus = new TextView(this);
        savedStatus.setText("🔒 आपकी API Key सुरक्षित रूप से सेव और लॉक है।");
        savedStatus.setTextColor(Color.parseColor("#137333"));
        savedStatus.setTextSize(14);
        savedStatus.setPadding(10, 15, 10, 15);
        savedStatus.setVisibility(View.GONE);
        layout.addView(savedStatus);

        final Button btnChangeKey = new Button(this);
        btnChangeKey.setText("की बदलें (Change Key)");
        btnChangeKey.setBackgroundColor(Color.parseColor("#5F6368"));
        btnChangeKey.setTextColor(Color.WHITE);
        btnChangeKey.setVisibility(View.GONE);
        layout.addView(btnChangeKey);

        final Button btnSave = new Button(this);
        btnSave.setText("✔ सेटिंग सुरक्षित करें (SAVE)");
        btnSave.setBackgroundColor(Color.parseColor("#1A73E8"));
        btnSave.setTextColor(Color.WHITE);
        layout.addView(btnSave);

        String existingKey = prefs.getString("user_gemini_key", "");
        if (!existingKey.isEmpty()) {
            keyContainer.setVisibility(View.GONE);
            savedStatus.setVisibility(View.VISIBLE);
            btnChangeKey.setVisibility(View.VISIBLE);
            btnSave.setVisibility(View.GONE);
        }

        btnSave.setOnClickListener(v -> {
            String keyInput = etKey.getText().toString().trim();
            prefs.edit()
                    .putString("assistant_name", etName.getText().toString().trim())
                    .putString("user_gemini_key", keyInput)
                    .apply();

            Toast.makeText(this, "की सुरक्षित हो गई!", Toast.LENGTH_SHORT).show();

            if (!keyInput.isEmpty()) {
                keyContainer.setVisibility(View.GONE);
                savedStatus.setVisibility(View.VISIBLE);
                btnChangeKey.setVisibility(View.VISIBLE);
                btnSave.setVisibility(View.GONE);
            }
        });

        btnChangeKey.setOnClickListener(v -> {
            keyContainer.setVisibility(View.VISIBLE);
            savedStatus.setVisibility(View.GONE);
            btnChangeKey.setVisibility(View.GONE);
            btnSave.setVisibility(View.VISIBLE);
            etKey.setText("");
            etKey.requestFocus();
        });

        TextView permLabel = new TextView(this);
        permLabel.setText("\nज़रूरी परमिशन चालू करें:");
        permLabel.setPadding(0, 20, 0, 5);
        layout.addView(permLabel);

        Button btnOverlay = new Button(this);
        btnOverlay.setText("1. स्क्रीन पर तैरने वाला बटन ALLOW करें");
        btnOverlay.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });
        layout.addView(btnOverlay);

        Button btnAccessibility = new Button(this);
        btnAccessibility.setText("2. ऑटो-क्लिक व सर्विस चालू करें");
        btnAccessibility.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });
        layout.addView(btnAccessibility);

        scrollView.addView(layout);
        setContentView(scrollView);
    }
}
