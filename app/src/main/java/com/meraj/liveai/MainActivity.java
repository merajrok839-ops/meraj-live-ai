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
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("AiPrefs", MODE_PRIVATE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 70, 50, 50);
        layout.setBackgroundColor(Color.parseColor("#F4F6F9"));

        TextView title = new TextView(this);
        title.setText("⚡ Meraj Live AI Setup");
        title.setTextSize(22);
        title.setTextColor(Color.parseColor("#1A73E8"));
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 30);
        layout.addView(title);

        // 1. असिस्टेंट का नाम रखने वाला बॉक्स
        TextView label = new TextView(this);
        label.setText("अपने AI असिस्टेंट का नाम रखें:");
        label.setTextSize(16);
        label.setTextColor(Color.BLACK);
        layout.addView(label);

        final EditText etName = new EditText(this);
        String savedName = prefs.getString("assistant_name", "Meraj AI");
        etName.setText(savedName);
        etName.setBackgroundColor(Color.WHITE);
        etName.setPadding(25, 25, 25, 25);
        layout.addView(etName);

        Button btnSaveName = new Button(this);
        btnSaveName.setText("नाम सुरक्षित करें (Save Name)");
        btnSaveName.setBackgroundColor(Color.parseColor("#1A73E8"));
        btnSaveName.setTextColor(Color.WHITE);
        btnSaveName.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (!name.isEmpty()) {
                prefs.edit().putString("assistant_name", name).apply();
                Toast.makeText(this, "असिस्टेंट का नाम सेट हुआ: " + name, Toast.LENGTH_SHORT).show();
            }
        });
        layout.addView(btnSaveName);

        TextView spacer = new TextView(this);
        spacer.setPadding(0, 30, 0, 0);
        layout.addView(spacer);

        // 2. फ्लोटिंग बटन परमिशन
        Button btnOverlay = new Button(this);
        btnOverlay.setText("1. स्क्रीन पर तैरने की अनुमति (Floating Button)");
        btnOverlay.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        });
        layout.addView(btnOverlay);

        // 3. ऑटो-क्लिक परमिशन
        Button btnAccessibility = new Button(this);
        btnAccessibility.setText("2. ऑटो-क्लिक और ऐप्स कंट्रोल अनुमति (Accessibility)");
        btnAccessibility.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "लिस्ट में 'Meraj Live AI' ढूंढकर चालू करें", Toast.LENGTH_LONG).show();
        });
        layout.addView(btnAccessibility);

        setContentView(layout);
    }
}
