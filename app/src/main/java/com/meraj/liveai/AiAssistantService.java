package com.meraj.liveai;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.Toast;

public class AiAssistantService extends AccessibilityService {

    private WindowManager windowManager;
    private Button floatingButton;
    public static AiAssistantService instance;
    private SharedPreferences prefs;

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        prefs = getSharedPreferences("AiPrefs", MODE_PRIVATE);
        String assistantName = prefs.getString("assistant_name", "Meraj AI");

        showToast(assistantName + " सर्विस एक्टिव है!");
        createFloatingWidget(assistantName);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // स्क्रीन इवेंट्स सुनने के लिए
    }

    @Override
    public void onInterrupt() {
        instance = null;
    }

    // --- 1. यूट्यूब पर गाना प्ले करने का ऑटोमैटिक फंक्शन ---
    public void playYoutubeSong(String songQuery) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEARCH);
            intent.setPackage("com.google.android.youtube");
            intent.putExtra("query", songQuery);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            showToast("यूट्यूब पर सर्च हो रहा है: " + songQuery);
        } catch (Exception e) {
            Intent webIntent = new Intent(Intent.ACTION_VIEW, 
                    Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(songQuery)));
            webIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(webIntent);
        }
    }

    // --- 2. स्क्रीन पर ऑटो-क्लिक (Auto Click) करने का फंक्शन ---
    public void autoClickAt(float x, float y) {
        Path clickPath = new Path();
        clickPath.moveTo(x, y);

        GestureDescription.StrokeDescription stroke = 
                new GestureDescription.StrokeDescription(clickPath, 0, 100);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(stroke);

        dispatchGesture(builder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                showToast("AI ने स्क्रीन पर क्लिक किया: (" + (int)x + ", " + (int)y + ")");
            }
        }, null);
    }

    // --- 3. स्क्रीन पर तैरने वाला फ्लोटिंग बबल ---
    private void createFloatingWidget(String name) {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        floatingButton = new Button(this);
        floatingButton.setText(name + " ⚡");
        floatingButton.setTextColor(Color.WHITE);
        floatingButton.setBackgroundColor(Color.parseColor("#1A73E8"));
        floatingButton.setPadding(30, 20, 30, 20);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 250;

        floatingButton.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingButton, params);
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (Math.abs(event.getRawX() - initialTouchX) < 15 && Math.abs(event.getRawY() - initialTouchY) < 15) {
                            onFloatingButtonClick();
                        }
                        return true;
                }
                return false;
            }
        });

        windowManager.addView(floatingButton, params);
    }

    // जब यूजर स्क्रीन पर तैरते हुए AI बटन पर टैप करे
    private void onFloatingButtonClick() {
        showToast("कमांड चालू: यूट्यूब पर किशोर दा का गाना प्ले हो रहा है...");
        playYoutubeSong("Kishore Kumar hit songs");
    }

    private void showToast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingButton != null && windowManager != null) {
            windowManager.removeView(floatingButton);
        }
        instance = null;
    }
}
