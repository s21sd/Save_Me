package com.mobile.saveme;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.provider.Telephony;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "SMS_Channel";
    private static final String CHANNEL_NAME = "SMS Notifications";
    private FusedLocationProviderClient fusedLocationClient;

    @Override
    public void onReceive(Context context, Intent intent) {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);

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
                String[] parts = message.split("Latitude: |, Longitude: ");

                String latitudeString = parts[1].trim();
                String longitudeString = parts[2].trim();

                double latitude = Double.parseDouble(latitudeString);
                double longitude = Double.parseDouble(longitudeString);


                Intent mapIntent = new Intent(context, MapActivity.class);
                mapIntent.putExtra("latitude", latitude);
                mapIntent.putExtra("longitude", longitude);
                mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                new Handler().postDelayed(() -> {
                    context.startActivity(mapIntent);
                },1000);
                LocalBroadcastManager.getInstance(context).sendBroadcast(broadcastIntent);
                showNotificationToStuckPerson(context,latitude,longitude,smsMessage.getDisplayOriginatingAddress());
            }
            else{
                double[] latLong = extractLatLong(message);
                if (latLong != null) {
                    showNotification(context, latLong[0], latLong[1], smsMessage.getDisplayOriginatingAddress());
                } else {
//                    Toast.makeText(context, "No valid coordinates found in the message", Toast.LENGTH_SHORT).show();
                }
            }

        }
    }

    // For the Helper Notification I will Show him like this
    private void showNotification(Context context, double latitude, double longitude, String phoneNumber) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        Intent choiceIntent = new Intent(context, ChoiceActivity.class);
        choiceIntent.putExtra("latitude", latitude);
        choiceIntent.putExtra("longitude", longitude);
        choiceIntent.putExtra("phoneNumber", phoneNumber);
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

    // For the person Who is stuck I will show the notification Like this
    private void  showNotificationToStuckPerson(Context context, double latitude, double longitude, String phoneNumber){
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        String messageToSend = "We will save you, be safe! Latitude: " + latitude + ", Longitude: " + longitude;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(messageToSend)
                .setContentText("Location: " + latitude + ", " + longitude)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        notificationManager.notify(0, builder.build());
    }

    private void handleSendSms(final Context context, Intent intent) {
        final String phoneNumber = intent.getStringExtra("phoneNumber");
        final String baseMessage = intent.getStringExtra("message");

        if (phoneNumber != null && baseMessage != null) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    String messageToSend = baseMessage + " Latitude: " + latitude + ", Longitude: " + longitude;


                    SmsManager smsManager = SmsManager.getDefault();
                    smsManager.sendTextMessage(phoneNumber, null, messageToSend, null, null);
                    Toast.makeText(context, "Message sent to " + phoneNumber, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(context, "Unable to get location.", Toast.LENGTH_SHORT).show();
                }
            });
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
