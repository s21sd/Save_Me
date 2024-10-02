package com.mobile.saveme;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Message {
    private String sender;
    private String message;
    private long timestamp;
    private boolean isSent;

    public Message(String sender, String message, long timestamp, boolean isSent) {
        this.sender = sender;
        this.message = message;
        this.timestamp = timestamp;
        this.isSent = isSent;
    }

    // Getters
    public String getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public boolean isSent() {
        return isSent;
    }

    // Setters (if needed)
    public void setMessage(String message) {
        this.message = message;
    }

    public void setSent(boolean sent) {
        isSent = sent;
    }

    // Method to get formatted timestamp
    public String getFormattedTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    @Override
    public String toString() {
        return "Message{" +
                "sender='" + sender + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                ", isSent=" + isSent +
                '}';
    }
}
