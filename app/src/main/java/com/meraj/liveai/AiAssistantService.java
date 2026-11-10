package com.meraj.liveai;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
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

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONObject;

public class AiAssistantService extends AccessibilityService {

    private WindowManager windowManager;
    private Button floatingBtn;
    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private Handler mainHandler;
    private SharedPreferences prefs;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

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

        mainHandler = new Handler(Looper.getMainLooper());
        prefs = getSharedPreferences("AIPrefs", Context.MODE_PRIVATE);

        initTTS();
        createFloatingButton();
    }

    private void initTTS() {
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(new Locale("hi", "IN"));
            }
        });
    }

    private void speak(String text) {
        mainHandler.post(() -> {
            if (tts != null) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "LiveAiUtterance");
            }
        });
    }

    private void createFloatingButton() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatingBtn = new Button(this);
        floatingBtn.setText("🎙️ MERAJ AI");
        floatingBtn.setTextColor(Color.WHITE);
        floatingBtn.setBackgroundColor(Color.parseColor("#1A73E8"));
        floatingBtn.setPadding(25, 15, 25, 15);

        final WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );

        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 50;
        params.y = 250;

        floatingBtn.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float touchX, touchY;
            private boolean isClick = true;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
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
                        params.x = initialX + dx;
                        params.y = initialY + dy;
                        windowManager.updateViewLayout(floatingBtn, params);
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (isClick) {
                            startListening();
                        }
                        return true;
                }
                return false;
            }
        });

        windowManager.addView(floatingBtn, params);
    }

    private void startListening() {
        mainHandler.post(() -> {
            if (speechRecognizer != null) {
                speechRecognizer.destroy();
            }
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN");

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) {
                    floatingBtn.setText("🔴 सुन रहा हूँ...");
                    floatingBtn.setBackgroundColor(Color.RED);
                }
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {
                    floatingBtn.setText("⏳ सोच रहा हूँ...");
                    floatingBtn.setBackgroundColor(Color.parseColor("#E37400"));
                }
                @Override public void onError(int error) {
                    resetButton();
                }
                @Override public void onResults(Bundle results) {
                    resetButton();
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        processVoiceCommand(matches.get(0));
                    }
                }
                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });

            speechRecognizer.startListening(intent);
        });
    }

    private void resetButton() {
        mainHandler.post(() -> {
            if (floatingBtn != null) {
                floatingBtn.setText("🎙️ MERAJ AI");
                floatingBtn.setBackgroundColor(Color.parseColor("#1A73E8"));
            }
        });
    }

    private void processVoiceCommand(String query) {
        String lower = query.toLowerCase();

        // 1. ऑटो क्लिक
        if (lower.startsWith("क्लिक करो") || lower.startsWith("दबाओ") || lower.contains("पर क्लिक करो")) {
            String target = query.replace("क्लिक करो", "").replace("दबाओ", "").replace("पर", "").trim();
            if (performClickOnText(target)) {
                speak(target + " पर क्लिक कर दिया");
                return;
            }
        }

        // 2. यूट्यूब
        if (lower.contains("यूट्यूब") || lower.contains("youtube") || lower.contains("गाना")) {
            speak("यूट्यूब पर चला रहा हूँ");
            String search = query.replace("यूट्यूब", "").replace("पर", "").replace("चलाओ", "").replace("सर्च करो", "").trim();
            Intent intent = new Intent(Intent.ACTION_SEARCH);
            intent.setPackage("com.google.android.youtube");
            intent.putExtra("query", search);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
            } catch (Exception e) {
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(search)));
                webIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(webIntent);
            }
            return;
        }

        // 3. व्हाट्सएप
        if (lower.contains("व्हाट्सएप") || lower.contains("whatsapp")) {
            speak("व्हाट्सएप खोल रहा हूँ");
            Intent launchIntent = getPackageManager().getLaunchIntentForPackage("com.whatsapp");
            if (launchIntent != null) {
                launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);
            }
            return;
        }

        // 4. लाइव स्क्रीन संदर्भ और जेमिनी सवाल
        String screenContext = extractScreenContext();
        askGemini(query, screenContext);
    }

    private boolean performClickOnText(String targetText) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || targetText.isEmpty()) return false;

        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(targetText);
        if (nodes != null && !nodes.isEmpty()) {
            for (AccessibilityNodeInfo node : nodes) {
                if (node.isClickable()) {
                    return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }
                AccessibilityNodeInfo parent = node.getParent();
                while (parent != null) {
                    if (parent.isClickable()) {
                        return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    }
                    parent = parent.getParent();
                }
            }
        }
        return false;
    }

    private String extractScreenContext() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return "स्क्रीन खाली है या होम स्क्रीन पर हैं।";

        StringBuilder sb = new StringBuilder();
        if (root.getPackageName() != null) {
            sb.append("वर्तमान ऐप: ").append(root.getPackageName().toString()).append("\nस्क्रीन टेक्स्ट: ");
        }
        traverseNode(root, sb);
        String result = sb.toString();
        if (result.length() > 1500) {
            result = result.substring(0, 1500);
        }
        return result;
    }

    private void traverseNode(AccessibilityNodeInfo node, StringBuilder sb) {
        if (node == null || sb.length() > 1500) return;
        if (node.getText() != null && node.getText().length() > 0) {
            sb.append(node.getText().toString()).append(" | ");
        }
        if (node.getContentDescription() != null && node.getContentDescription().length() > 0) {
            sb.append(node.getContentDescription().toString()).append(" | ");
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            traverseNode(node.getChild(i), sb);
        }
    }

    private void askGemini(String userQuery, String screenContext) {
        String apiKey = prefs.getString("user_gemini_key", "").trim();

        if (apiKey.isEmpty()) {
            speak("कृपया पहले ऐप खोलकर अपनी Gemini API Key सेव करें।");
            return;
        }

        new Thread(() -> {
            try {
                String prompt = "यूजर का सवाल: " + userQuery +
                        "\n\nयूजर के मोबाइल स्क्रीन पर वर्तमान में खुला विवरण:\n" + screenContext +
                        "\n\nनिर्देश: यूजर को हिंदी में 1-2 पंक्तियों में सटीक, दोस्ताना और सीधा उत्तर दें। यदि यूजर पूछे कि 'मैं कहाँ हूँ', तो वर्तमान ऐप और स्क्रीन का नाम बताएं।";

                boolean isAuthKey = apiKey.startsWith("AQ.");
                String urlString;

                // AQ. Auth key के लिए URL में कभी ?key= नहीं जोड़ना है
                if (isAuthKey) {
                    urlString = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent";
                } else {
                    urlString = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + apiKey;
                }

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                // AQ. key के लिए केवल Authorization Header जाएगा
                if (isAuthKey) {
                    conn.setRequestProperty("Authorization", "Bearer " + apiKey);
                } else {
                    conn.setRequestProperty("x-goog-api-key", apiKey);
                }

                conn.setDoOutput(true);
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                JSONObject textPart = new JSONObject();
                textPart.put("text", prompt);

                JSONArray parts = new JSONArray();
                parts.put(textPart);

                JSONObject content = new JSONObject();
                content.put("parts", parts);

                JSONArray contents = new JSONArray();
                contents.put(content);

                JSONObject body = new JSONObject();
                body.put("contents", contents);

                byte[] postBytes = body.toString().getBytes("UTF-8");
                conn.setFixedLengthStreamingMode(postBytes.length);

                OutputStream os = conn.getOutputStream();
                os.write(postBytes);
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                if (responseCode == 200) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder res = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) res.append(line);
                    br.close();

                    JSONObject jsonResponse = new JSONObject(res.toString());
                    String reply = jsonResponse.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");

                    speak(reply.trim());
                } else {
                    speak("गूगल सर्वर एरर कोड " + responseCode + " आया है। कृपया अपनी की जांचें।");
                }
            } catch (Exception e) {
                speak("माफ़ कीजिये, नेटवर्क कनेक्शन में समस्या आ रही है।");
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingBtn != null && windowManager != null) {
            windowManager.removeView(floatingBtn);
        }
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }
}
