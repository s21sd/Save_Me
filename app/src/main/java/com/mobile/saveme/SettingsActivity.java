package com.mobile.saveme;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewContacts;
    private ContactAdapter contactAdapter;
    private List<Contact> contactList;
    private EditText etName, etPhoneNumber;
    private Button btnAddContact;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        recyclerViewContacts = findViewById(R.id.recyclerViewContacts);
        etName = findViewById(R.id.etName);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        btnAddContact = findViewById(R.id.btnAddContact);

        sharedPreferences = getSharedPreferences("contacts_prefs", Context.MODE_PRIVATE);
        contactList = loadContactsFromPreferences();

        // Initialize the adapter and RecyclerView
        contactAdapter = new ContactAdapter(contactList, new ContactAdapter.OnItemLongClickListener() {
            @Override
            public void onItemLongClick(int position) {
                deleteContact(position);
            }
        });
        recyclerViewContacts.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewContacts.setAdapter(contactAdapter);

        // Ensure the default contact is present
        ensureDefaultContact();

        btnAddContact.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addContact();
            }
        });
    }

    private void addContact() {
        String name = etName.getText().toString().trim();
        String phoneNumber = etPhoneNumber.getText().toString().trim();

        if (name.isEmpty() || phoneNumber.isEmpty()) {
            Toast.makeText(SettingsActivity.this, "Please enter both name and phone number.", Toast.LENGTH_SHORT).show();
            return;
        }

        Contact newContact = new Contact(name, phoneNumber);
        contactList.add(newContact);
        contactAdapter.notifyDataSetChanged(); // Notify adapter of changes
        saveContactToPreferences(name, phoneNumber);

        etName.setText("");
        etPhoneNumber.setText("");
    }

    private void deleteContact(int position) {
        Contact contact = contactList.get(position);
        if (contact.getName().equals("Save_Me_Company")) {
            Toast.makeText(SettingsActivity.this, "Cannot delete default contact.", Toast.LENGTH_SHORT).show();
            return;
        }

        contactList.remove(position);
        contactAdapter.notifyDataSetChanged(); // Notify adapter of changes
        saveContactsToPreferences();
    }

    private void saveContactToPreferences(String name, String phoneNumber) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String contactListString = sharedPreferences.getString("contact_list", "");
        if (!contactListString.isEmpty()) {
            contactListString += ",";
        }
        contactListString += name + ":" + phoneNumber;
        editor.putString("contact_list", contactListString);
        editor.apply();
    }

    private void saveContactsToPreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        StringBuilder contactListString = new StringBuilder();
        for (Contact contact : contactList) {
            if (contactListString.length() > 0) {
                contactListString.append(",");
            }
            contactListString.append(contact.getName()).append(":").append(contact.getPhoneNumber());
        }
        editor.putString("contact_list", contactListString.toString());
        editor.apply();
    }

    private List<Contact> loadContactsFromPreferences() {
        List<Contact> contacts = new ArrayList<>();
        String contactListString = sharedPreferences.getString("contact_list", "");
        if (!contactListString.isEmpty()) {
            String[] contactsArray = contactListString.split(",");
            for (String contact : contactsArray) {
                String[] contactDetails = contact.split(":");
                if (contactDetails.length == 2) {
                    contacts.add(new Contact(contactDetails[0], contactDetails[1]));
                }
            }
        }
        return contacts;
    }

    private void ensureDefaultContact() {
        boolean defaultContactExists = false;
        for (Contact contact : contactList) {
            if (contact.getName().equals("Save_Me_Company")) {
                defaultContactExists = true;
                break;
            }
        }

        if (!defaultContactExists) {
            contactList.add(new Contact("Save_Me_Company", "7905280916"));
            contactAdapter.notifyDataSetChanged(); // Notify adapter of changes
            saveContactToPreferences("Save_Me_Company", "7905280916");
        }
    }
}
