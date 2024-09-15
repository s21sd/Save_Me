package com.mobile.saveme;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.app.Service;
import android.os.IBinder;
import android.widget.Toast;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.Locale;

public class VoiceService extends Service {

    private SpeechRecognizer speechRecognizer;
    private static final String CHANNEL_ID = "VoiceServiceChannel";
    private static final String TAG = "VoiceService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service Created");
        createNotificationChannel();
        initializeSpeechRecognizer();
        startListening();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Voice Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
            Log.d(TAG, "Notification Channel Created");
        }
    }

    private void initializeSpeechRecognizer() {
        Log.d(TAG, "Initializing Speech Recognizer");
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d(TAG, "Ready for Speech");
            }

            @Override
            public void onBeginningOfSpeech() {
                Log.d(TAG, "Speech Beginning");
            }

            @Override
            public void onRmsChanged(float rmsdB) {
                Log.d(TAG, "RMS Changed: " + rmsdB);
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                Log.d(TAG, "Buffer Received");
            }

            @Override
            public void onEndOfSpeech() {
                Log.d(TAG, "End of Speech");
                startListening();
            }

            @Override
            public void onError(int error) {
                Log.d(TAG, "Speech Recognition Error: " + error);
                startListening();
            }

            @Override
            public void onResults(Bundle results) {
                Log.d(TAG, "Speech Recognition Results");
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null) {
                    for (String match : matches) {
                        Log.d(TAG, "Match: " + match);
                        if (match.equalsIgnoreCase("help") || match.equalsIgnoreCase("help me")) {
                            // Trigger action, for example, start the app
                            Log.d("Got",match);

                        }
                    }
                }
                startListening();
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                Log.d(TAG, "Partial Results");
            }

            @Override
            public void onEvent(int eventType, Bundle params) {
                Log.d(TAG, "Event: " + eventType);
            }
        });
    }

    private void startListening() {
        Log.d(TAG, "Starting Listening");
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        speechRecognizer.startListening(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service Started");
        // Create a notification to show the service is running
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Voice Service")
                .setContentText("Listening for 'help' command...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(1, notification);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service Destroyed");
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

