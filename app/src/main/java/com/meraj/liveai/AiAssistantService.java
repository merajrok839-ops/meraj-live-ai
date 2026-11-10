package com.meraj.liveai;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class AiAssistantService extends AccessibilityService implements TextToSpeech.OnInitListener {

    private WindowManager windowManager;
    private Button floatingButton;
    private SharedPreferences prefs;
    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private Handler mainHandler;
    private boolean isListening = false;

    private static final String DEFAULT_FREE_KEY = ""; 

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        prefs = getSharedPreferences("AiPrefs", MODE_PRIVATE);
        mainHandler = new Handler(Looper.getMainLooper());
        tts = new TextToSpeech(this, this);

        String assistantName = prefs.getString("assistant_name", "Meraj AI");
        createFloatingWidget(assistantName);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(new Locale("hi", "IN"));
        }
    }

    private void speak(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    // --- 1. स्क्रीन पर तैरने वाला बटन ---
    private void createFloatingWidget(String name) {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatingButton = new Button(this);
        floatingButton.setText("🎙️ " + name);
        floatingButton.setTextColor(Color.WHITE);
        floatingButton.setBackgroundColor(Color.parseColor("#1A73E8"));
        floatingButton.setPadding(25, 20, 25, 20);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 300;

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
                            toggleVoiceListening();
                        }
                        return true;
                }
                return false;
            }
        });

        windowManager.addView(floatingButton, params);
    }

    // --- 2. लाइव आवाज सुनना ---
    private void toggleVoiceListening() {
        if (isListening) return;

        mainHandler.post(() -> {
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
                    speechRecognizer.setRecognitionListener(new RecognitionListener() {
                        @Override public void onReadyForSpeech(Bundle params) { floatingButton.setText("🔴 सुन रहा हूँ..."); }
                        @Override public void onBeginningOfSpeech() {}
                        @Override public void onRmsChanged(float rmsdB) {}
                        @Override public void onBufferReceived(byte[] buffer) {}
                        @Override public void onEndOfSpeech() { floatingButton.setText("⚡ सोच रहा हूँ..."); }
                        @Override public void onError(int error) {
                            isListening = false;
                            floatingButton.setText("🎙️ " + prefs.getString("assistant_name", "Meraj AI"));
                        }
                        @Override public void onResults(Bundle results) {
                            isListening = false;
                            floatingButton.setText("🎙️ " + prefs.getString("assistant_name", "Meraj AI"));
                            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                            if (matches != null && !matches.isEmpty()) {
                                handleUserVoiceCommand(matches.get(0));
                            }
                        }
                        @Override public void onPartialResults(Bundle partialResults) {}
                        @Override public void onEvent(int eventType, Bundle params) {}
                    });
                }

                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
                intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "बोलिए क्या करना है...");
                speechRecognizer.startListening(intent);
                isListening = true;
            } catch (Exception e) {
                isListening = false;
                Toast.makeText(this, "माइक शुरू नहीं हो सका", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- 3. स्क्रीन की जानकारी पढ़ना ---
    private String getScreenText() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return "स्क्रीन खाली या लॉक है";
        StringBuilder sb = new StringBuilder();
        extractText(root, sb);
        return sb.toString();
    }

    private void extractText(AccessibilityNodeInfo node, StringBuilder sb) {
        if (node == null) return;
        if (node.getText() != null) {
            sb.append(node.getText().toString()).append(" | ");
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            extractText(node.getChild(i), sb);
        }
    }

    // --- 4. आवाज के अनुसार काम करना ---
    private void handleUserVoiceCommand(String query) {
        String lower = query.toLowerCase();

        // YouTube पर गाना
        if (lower.contains("यूट्यूब") || lower.contains("youtube") || lower.contains("गाना") || lower.contains("सॉन्ग")) {
            String song = query.replace("यूट्यूब", "").replace("youtube", "").replace("पर", "").replace("चलाओ", "").replace("लगाओ", "").trim();
            speak(song + " प्ले कर रहा हूँ");
            playYoutube(song);
            return;
        }

        // Facebook पर सर्च
        if (lower.contains("फेसबुक") || lower.contains("facebook")) {
            String person = query.replace("फेसबुक", "").replace("facebook", "").replace("पर", "").replace("सर्च करो", "").replace("ढूंढो", "").trim();
            speak("फेसबुक पर " + person + " खोज रहा हूँ");
            openFacebook(person);
            return;
        }

        // WhatsApp खोलना
        if (lower.contains("व्हाट्सएप") || lower.contains("whatsapp")) {
            speak("व्हाट्सएप खोल रहा हूँ");
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.whatsapp");
            if (launchIntent != null) {
                launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);
            }
            return;
        }

        // स्क्रीन पर क्या है / मैं कहाँ हूँ / अन्य सवाल -> Gemini AI
        String screenData = getScreenText();
        askGeminiAi(query, screenData);
    }

    private void playYoutube(String query) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEARCH);
            intent.setPackage("com.google.android.youtube");
            intent.putExtra("query", query);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse("https://m.youtube.com/results?search_query=" + Uri.encode(query)));
            web.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(web);
        }
    }

    private void openFacebook(String query) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("fb://facewebmodal/f?href=https://m.facebook.com/search/top/?q=" + Uri.encode(query)));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (Exception e) {
            Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse("https://m.facebook.com/search/top/?q=" + Uri.encode(query)));
            web.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(web);
        }
    }

    // --- 5. Gemini AI इंजन (3 फ्री ट्रायल + पर्सनल API Key) ---
    private void askGeminiAi(String prompt, String screenContext) {
        int trials = prefs.getInt("free_trials_used", 0);
        String userKey = prefs.getString("user_gemini_key", "").trim();

        if (trials >= 3 && userKey.isEmpty()) {
            speak("आपके 3 फ्री काम पूरे हो चुके हैं। कृपया ऐप खोलकर अपनी फ्री जेमिनी की डालें।");
            return;
        }

        new Thread(() -> {
            try {
                String fullPrompt = "यूजर की मोबाइल स्क्रीन पर यह टेक्स्ट दिख रहा है: [" + screenContext + "]. यूजर का सवाल: '" + prompt + "'. जवाब बहुत छोटा और साफ हिंदी में दो।";

                String keyToUse = userKey.isEmpty() ? DEFAULT_FREE_KEY : userKey;

                if (keyToUse.isEmpty()) {
                    prefs.edit().putInt("free_trials_used", trials + 1).apply();
                    mainHandler.post(() -> speak("स्क्रीन पर यह ऐप खुला हुआ है। फ्री ट्रायल " + (trials + 1) + " पूरा हुआ।"));
                    return;
                }

                URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + keyToUse);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                JSONObject textPart = new JSONObject().put("text", fullPrompt);
                JSONObject parts = new JSONObject().put("parts", new JSONArray().put(textPart));
                JSONObject body = new JSONObject().put("contents", new JSONArray().put(parts));

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes("UTF-8"));
                os.close();

                if (conn.getResponseCode() == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder res = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) res.append(line);
                    br.close();

                    JSONObject json = new JSONObject(res.toString());
                    String aiResponse = json.getJSONArray("candidates").getJSONObject(0)
                            .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text");

                    prefs.edit().putInt("free_trials_used", trials + 1).apply();
                    mainHandler.post(() -> speak(aiResponse));
                } else {
                    mainHandler.post(() -> speak("माफ़ कीजिये, जवाब प्राप्त नहीं हो सका।"));
                }
            } catch (Exception e) {
                mainHandler.post(() -> speak("इंटरनेट कनेक्शन चेक करें।"));
            }
        }).start();
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override public void onInterrupt() {}

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingButton != null && windowManager != null) windowManager.removeView(floatingButton);
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (speechRecognizer != null) speechRecognizer.destroy();
    }
}
