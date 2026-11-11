package com.meraj.liveai; // अपनी पहली लाइन वाला पैकेज नाम यही रहने दें

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class AiAssistantService extends AccessibilityService implements TextToSpeech.OnInitListener {

    private static final String TAG = "MerajLiveAI";
    private WindowManager windowManager;
    private Button floatingButton;
    private WindowManager.LayoutParams layoutParams;

    private SpeechRecognizer speechRecognizer;
    private TextToSpeech textToSpeech;
    private Handler mainHandler;

    private boolean isListening = false;

    @Override
    public void onCreate() {
        super.onCreate();
        mainHandler = new Handler(Looper.getMainLooper());
        textToSpeech = new TextToSpeech(this, this);

        createFloatingButton();
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                     AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS |
                     AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // स्क्रीन इवेंट्स बैकग्राउंड में ट्रैक होते रहते हैं
    }

    @Override
    public void onInterrupt() {
        Log.e(TAG, "Service Interrupted");
    }

    // --- 1. फ्लोटिंग बटन (ड्रैग व स्टेटस कंट्रोल) ---
    private void createFloatingButton() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatingButton = new Button(this);
        setButtonState("🎙️ MERAJ AI", "#1E1E2E");

        int layoutType = (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        layoutParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        layoutParams.gravity = Gravity.TOP | Gravity.START;
        layoutParams.x = 40;
        layoutParams.y = 250;

        // स्क्रीन पर उँगली से कहीं भी घुमाने (Drag) और टैप करने का लॉजिक
        floatingButton.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float touchX, touchY;
            private boolean isClick = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = layoutParams.x;
                        initialY = layoutParams.y;
                        touchX = event.getRawX();
                        touchY = event.getRawY();
                        isClick = true;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        int dx = (int) (event.getRawX() - touchX);
                        int dy = (int) (event.getRawY() - touchY);
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isClick = false;
                        }
                        layoutParams.x = initialX + dx;
                        layoutParams.y = initialY + dy;
                        windowManager.updateViewLayout(floatingButton, layoutParams);
                        return true;

                    case MotionEvent.ACTION_UP:
                        if (isClick) {
                            onFloatingButtonClick();
                        }
                        return true;
                }
                return false;
            }
        });

        windowManager.addView(floatingButton, layoutParams);
    }

    private void setButtonState(String text, String colorHex) {
        mainHandler.post(() -> {
            floatingButton.setText(text);
            floatingButton.setTextColor(Color.WHITE);
            floatingButton.setTextSize(13);
            floatingButton.setPadding(30, 20, 30, 20);

            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.RECTANGLE);
            shape.setCornerRadius(50f);
            shape.setColor(Color.parseColor(colorHex));
            shape.setStroke(3, Color.parseColor("#4A90E2"));
            floatingButton.setBackground(shape);
        });
    }

    private void onFloatingButtonClick() {
        if (isListening) return;
        startVoiceRecognition();
    }

    // --- 2. वॉयस इनपुट (हिंदी) ---
    private void startVoiceRecognition() {
        mainHandler.post(() -> {
            try {
                if (speechRecognizer != null) {
                    speechRecognizer.destroy();
                }

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN");

                speechRecognizer.setRecognitionListener(new RecognitionListener() {
                    @Override
                    public void onReadyForSpeech(Bundle params) {
                        isListening = true;
                        setButtonState("🔴 सुन रहा हूँ...", "#E53935");
                    }

                    @Override
                    public void onResults(Bundle results) {
                        isListening = false;
                        setButtonState("⏳ सोच रहा हूँ...", "#FB8C00");

                        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                        if (matches != null && !matches.isEmpty()) {
                            String userPrompt = matches.get(0);
                            handleUserQuery(userPrompt);
                        } else {
                            setButtonState("🎙️ MERAJ AI", "#1E1E2E");
                        }
                    }

                    @Override
                    public void onError(int error) {
                        isListening = false;
                        setButtonState("🎙️ MERAJ AI", "#1E1E2E");
                        showToast("माइक एरर कोड: " + error);
                    }

                    @Override public void onBeginningOfSpeech() {}
                    @Override public void onRmsChanged(float rmsdB) {}
                    @Override public void onBufferReceived(byte[] buffer) {}
                    @Override public void onEndOfSpeech() {}
                    @Override public void onPartialResults(Bundle partialResults) {}
                    @Override public void onEvent(int eventType, Bundle params) {}
                });

                speechRecognizer.startListening(intent);

            } catch (Exception e) {
                isListening = false;
                setButtonState("🎙️ MERAJ AI", "#1E1E2E");
                showToast("माइक शुरू नहीं हुआ: " + e.getMessage());
            }
        });
    }

    // --- 3. स्क्रीन टेक्स्ट पढ़ना और सवाल भेजना ---
    private void handleUserQuery(String userQuery) {
        // स्क्रीन का टेक्स्ट निकालना
        String screenText = extractScreenText();
        
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("तुम MERAJ LIVE AI वॉइस असिस्टेंट हो। हिंदी में छोटा, सटीक और साफ़ जवाब दो।\n");

        if (screenText.length() > 0) {
            promptBuilder.append("यूजर के फोन स्क्रीन पर अभी यह दिख रहा है: [")
                         .append(screenText)
                         .append("]\n");
        }

        promptBuilder.append("यूजर का सवाल: ").append(userQuery);

        callGeminiApi(promptBuilder.toString());
    }

    private String extractScreenText() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return "";

        StringBuilder sb = new StringBuilder();
        traverseNodes(rootNode, sb);
        rootNode.recycle();
        return sb.toString().trim();
    }

    private void traverseNodes(AccessibilityNodeInfo node, StringBuilder sb) {
        if (node == null) return;

        if (node.getText() != null && node.getText().length() > 0) {
            sb.append(node.getText().toString()).append(" ");
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                traverseNodes(child, sb);
                child.recycle();
            }
        }
    }

    // --- 4. Google Gemini API कॉल (401 एरर फिक्स) ---
    private void callGeminiApi(String finalPrompt) {
        new Thread(() -> {
            SharedPreferences prefs = getSharedPreferences("MerajAiPrefs", Context.MODE_PRIVATE);
            String apiKey = prefs.getString("GEMINI_API_KEY", "").trim();

            if (apiKey.isEmpty()) {
                setButtonState("🎙️ MERAJ AI", "#1E1E2E");
                showToast("Error: ऐप में जाकर API Key सेव करें!");
                speak("कृपया ऐप में अपनी एपीआई की सेव करें।");
                return;
            }

            HttpURLConnection conn = null;
            try {
                // डायरेक्ट URL में key जोड़ना (401 का अचूक समाधान)
                String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;
                URL url = new URL(endpoint);
                conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setDoOutput(true);
                conn.setDoInput(true);

                // JSON रिक्वेस्ट बॉडी
                JSONObject textPart = new JSONObject();
                textPart.put("text", finalPrompt);

                JSONArray partsArray = new JSONArray();
                partsArray.put(textPart);

                JSONObject contentObj = new JSONObject();
                contentObj.put("parts", partsArray);

                JSONArray contentsArray = new JSONArray();
                contentsArray.put(contentObj);

                JSONObject requestBody = new JSONObject();
                requestBody.put("contents", contentsArray);

                // रिक्वेस्ट भेजें
                OutputStream os = conn.getOutputStream();
                os.write(requestBody.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                if (responseCode == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) {
                        response.append(line);
                    }
                    in.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    String aiReply = jsonResponse.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");

                    speak(aiReply.trim());

                } else {
                    InputStream err = conn.getErrorStream();
                    String errDetail = "";
                    if (err != null) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(err));
                        StringBuilder sb = new StringBuilder();
                        String l;
                        while ((l = reader.readLine()) != null) sb.append(l);
                        reader.close();
                        errDetail = sb.toString();
                    }
                    Log.e(TAG, "API Error " + responseCode + ": " + errDetail);
                    showToast("API एरर " + responseCode + ": अपनी Key चेक करें");
                    speak("एपीआई एरर आया है। कृपया अपनी की जांचें।");
                }

            } catch (Exception e) {
                Log.e(TAG, "Connection Error", e);
                showToast("कनेक्शन एरर: " + e.getMessage());
            } finally {
                if (conn != null) conn.disconnect();
                setButtonState("🎙️ MERAJ AI", "#1E1E2E");
            }
        }).start();
    }

    // --- 5. टेक्स्ट-टू-स्पीच (TTS) ---
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = textToSpeech.setLanguage(new Locale("hi", "IN"));
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech.setLanguage(Locale.US);
            }
        }
    }

    private void speak(String text) {
        mainHandler.post(() -> {
            if (textToSpeech != null) {
                textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "MerajTtsId");
            }
        });
    }

    private void showToast(String msg) {
        mainHandler.post(() -> Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingButton != null && windowManager != null) {
            windowManager.removeView(floatingButton);
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
    }
}
