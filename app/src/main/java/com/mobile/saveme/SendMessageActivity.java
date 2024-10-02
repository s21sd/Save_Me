package com.mobile.saveme;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SendMessageActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ChatAdapter adapter;
    private List<Message> messageList;
    private MessageStorage messageStorage;
    EditText messageUser;
    FrameLayout sendMsgBtn;
    ImageView btnBack;
    String phoneNumber;

    private static final int SMS_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_send_message);

        checkSmsPermission();
        btnBack=findViewById(R.id.btnBack);
        recyclerView = findViewById(R.id.recyclerView);
        messageUser = findViewById(R.id.messageUser);
        sendMsgBtn = findViewById(R.id.sendButton);
        messageStorage = new MessageStorage(this);

        Intent intent = getIntent();
        String senderPhoneNumber = intent.getStringExtra("senderPhoneNumber");

        if (senderPhoneNumber != null) {
            phoneNumber = senderPhoneNumber;
        }
        btnBack.setOnClickListener(view -> {
            Intent intent3 = new Intent(SendMessageActivity.this, MapActivity.class);
            startActivity(intent3);
            finish();
        });

        messageList = messageStorage.getMessages();

        sendMsgBtn.setOnClickListener(view -> {
            String userMessage = messageUser.getText().toString();
            if (!userMessage.isEmpty()) {
                Message newMessage = new Message("Me", userMessage, 12, true);
                messageStorage.addMessage(newMessage);
                Log.e("Phone",phoneNumber);
                handleSendSms(this, userMessage);
                refreshMessages();
                messageUser.setText("");
            }
        });

        Log.d("SendMessageActivity", "Message List Size: " + messageList.size());

        adapter = new ChatAdapter(messageList);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
    }

    private void checkSmsPermission() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.SEND_SMS}, SMS_PERMISSION_CODE);
        }
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == SMS_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(this, "SMS Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "SMS Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void refreshMessages() {
        messageList.clear();
        messageList.addAll(messageStorage.getMessages());
        adapter.notifyDataSetChanged();
    }

    private void handleSendSms(final Context context, String message) {
        if (phoneNumber != null && message != null) {
            try {
                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
                Toast.makeText(context, "Message sent to " + phoneNumber, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e("SendMessageActivity", "SMS sending failed: " + e.getMessage());
                Toast.makeText(context, "Failed to send message: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "Failed to send message. Phone number or message is null.", Toast.LENGTH_SHORT).show();
        }
    }
}
