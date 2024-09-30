package com.mobile.saveme;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "SMS_Channel";
    private static final String CHANNEL_NAME = "SMS Notifications";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            handleReceivedSms(context, intent);
        } else if ("SEND_SMS".equals(intent.getAction())) {
            handleSendSms(context, intent);
        }
    }

    private void handleReceivedSms(Context context, Intent intent) {
        for (SmsMessage smsMessage : Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
            String message = smsMessage.getMessageBody();
            Log.e("TAG", "onReceive: " + message);

            if (message.contains("We will save you, be safe!")) {

                Intent broadcastIntent = new Intent("STOP_ANIMATION");
                broadcastIntent.putExtra("updateText", "Found");
                Toast.makeText(context, "We found you. Help is on the way.", Toast.LENGTH_SHORT).show();

                // Now from Here I have to send the user in the Map activity...
                LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
            }

            double[] latLong = extractLatLong(message);
            if (latLong != null) {
                showNotification(context, latLong[0], latLong[1], smsMessage.getDisplayOriginatingAddress());
            } else {
                Toast.makeText(context, "No valid coordinates found in the message", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private void showNotification(Context context, double latitude, double longitude, String phoneNumber) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        Intent choiceIntent = new Intent(context, ChoiceActivity.class);
        choiceIntent.putExtra("latitude", latitude);
        choiceIntent.putExtra("longitude", longitude);
        choiceIntent.putExtra("phoneNumber",phoneNumber);
        choiceIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingChoiceIntent = PendingIntent.getActivity(context, 0, choiceIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);


        String messageToSend = "We will save you, be safe!";

        Intent smsIntent = new Intent(context, MessageReceiver.class);
        smsIntent.setAction("SEND_SMS");
        smsIntent.putExtra("phoneNumber", phoneNumber);
        smsIntent.putExtra("message", messageToSend);
        PendingIntent pendingSmsIntent = PendingIntent.getBroadcast(context, 1, smsIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle("Need Help!")
                .setContentText("Location: " + latitude + ", " + longitude)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingChoiceIntent)
                .addAction(R.drawable.baseline_add_location_alt_24, "Send Message", pendingSmsIntent)
                .addAction(R.drawable.baseline_add_location_alt_24, "See in App", pendingChoiceIntent)
                .addAction(R.drawable.baseline_add_location_alt_24, "Open in Google Maps", pendingChoiceIntent);

        notificationManager.notify(0, builder.build());
    }

    private void handleSendSms(Context context, Intent intent) {
        String phoneNumber = intent.getStringExtra("phoneNumber");
        String message = intent.getStringExtra("message");

        if (phoneNumber != null && message != null) {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            Toast.makeText(context, "Message sent to " + phoneNumber, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Failed to send message.", Toast.LENGTH_SHORT).show();
        }
    }

    private double[] extractLatLong(String message) {
        Pattern pattern = Pattern.compile("lat:\\s*(-?\\d+\\.\\d+),\\s*log:\\s*(-?\\d+\\.\\d+)");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            try {
                double latitude = Double.parseDouble(matcher.group(1));
                double longitude = Double.parseDouble(matcher.group(2));
                return new double[]{latitude, longitude};
            } catch (NumberFormatException e) {
                Log.e("MessageReceiver", "Error parsing latitude or longitude: " + e.getMessage());
            }
        }
        return null;
    }
}
