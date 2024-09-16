package com.mobile.saveme;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ContactAdapter extends RecyclerView.Adapter<ContactAdapter.ContactViewHolder> {

    private final List<Contact> contactList;
    private final OnItemLongClickListener onItemLongClickListener;

    public ContactAdapter(List<Contact> contactList, OnItemLongClickListener onItemLongClickListener) {
        this.contactList = contactList;
        this.onItemLongClickListener = onItemLongClickListener;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the contact_item layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.contact_item, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        // Bind the contact data to the view holder
        Contact contact = contactList.get(position);
        holder.bind(contact);
    }

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    public class ContactViewHolder extends RecyclerView.ViewHolder {

        private final TextView nameTextView;
        private final TextView phoneNumberTextView;

        public ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize views
            nameTextView = itemView.findViewById(R.id.contactName);
            phoneNumberTextView = itemView.findViewById(R.id.contactPhoneNumber);

            // Set up the long click listener
            itemView.setOnLongClickListener(v -> {
                if (onItemLongClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        onItemLongClickListener.onItemLongClick(position);
                        return true; // Indicate that the long click was handled
                    }
                }
                return false; // Indicate that the long click was not handled
            });
        }

        public void bind(Contact contact) {
            nameTextView.setText(contact.getName());
            phoneNumberTextView.setText(contact.getPhoneNumber());
        }
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(int position);
    }
}
