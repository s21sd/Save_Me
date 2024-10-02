package com.mobile.saveme;

import android.content.Context;
import android.content.SharedPreferences;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MessageStorage {
    private static final String PREFS_NAME = "chat_preferences";
    private static final String MESSAGES_KEY = "messages";

    private SharedPreferences sharedPreferences;
    private Gson gson;

    public MessageStorage(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }


    public void saveMessages(List<Message> messageList) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String json = gson.toJson(messageList);
        editor.putString(MESSAGES_KEY, json);
        editor.apply();
    }

    public List<Message> getMessages() {
        String json = sharedPreferences.getString(MESSAGES_KEY, "");
        if (!json.isEmpty()) {
            Type type = new TypeToken<List<Message>>() {}.getType();
            return gson.fromJson(json, type);
        }
        return new ArrayList<>();
    }


    public void addMessage(Message message) {
        List<Message> messageList = getMessages();
        messageList.add(message);
        saveMessages(messageList);
    }
}
