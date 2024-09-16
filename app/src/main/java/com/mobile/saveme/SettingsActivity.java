package com.mobile.saveme;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private RecyclerView recyclerViewContacts;
    private ContactAdapter contactAdapter;
    private List<Contact> contactList;
    private EditText etName, etPhoneNumber;
    private Button btnAddContact, btnLogout;
    private SharedPreferences sharedPreferences;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        recyclerViewContacts = findViewById(R.id.recyclerViewContacts);
        etName = findViewById(R.id.etName);
        etPhoneNumber = findViewById(R.id.etPhoneNumber);
        btnAddContact = findViewById(R.id.btnAddContact);
        btnLogout = findViewById(R.id.btnLogout);

        sharedPreferences = getSharedPreferences("contacts_prefs", Context.MODE_PRIVATE);
        mAuth = FirebaseAuth.getInstance();
        contactList = loadContactsFromPreferences();

        contactAdapter = new ContactAdapter(contactList, this::deleteContact);
        recyclerViewContacts.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewContacts.setAdapter(contactAdapter);

        ensureDefaultContact();

        btnAddContact.setOnClickListener(v -> addContact());
        btnLogout.setOnClickListener(v -> logoutUser());
    }

    private void addContact() {
        String name = etName.getText().toString().trim();
        String phoneNumber = etPhoneNumber.getText().toString().trim();

        if (name.isEmpty() || phoneNumber.isEmpty()) {
            Toast.makeText(SettingsActivity.this, "Please enter both name and phone number.", Toast.LENGTH_SHORT).show();
            return;
        }

        Contact newContact = new Contact(name, phoneNumber);
        if (contactList.stream().noneMatch(c -> c.getPhoneNumber().equals(phoneNumber))) {
            contactList.add(newContact);
            saveContactsToPreferences();
            contactAdapter.notifyDataSetChanged();

            etName.setText("");
            etPhoneNumber.setText("");
        } else {
            Toast.makeText(SettingsActivity.this, "Contact already exists.", Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteContact(int position) {
        Contact contact = contactList.get(position);
        if (contact.getName().equals("Save_Me_Company")) {
            Toast.makeText(SettingsActivity.this, "Cannot delete default contact.", Toast.LENGTH_SHORT).show();
            return;
        }

        contactList.remove(position);
        saveContactsToPreferences(); // Save updated list
        contactAdapter.notifyDataSetChanged();
    }

    private void saveContactsToPreferences() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(contactList); // Convert contact list to JSON
        editor.putString("contactList", json); // Save JSON string
        editor.apply();
    }

    private List<Contact> loadContactsFromPreferences() {
        List<Contact> contacts = new ArrayList<>();
        String json = sharedPreferences.getString("contactList", null);

        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<List<Contact>>() {}.getType();
            contacts = gson.fromJson(json, type); // Convert JSON string back to list
        }

        return contacts;
    }

    private void ensureDefaultContact() {
        boolean defaultContactExists = contactList.stream()
                .anyMatch(contact -> contact.getName().equals("Save_Me_Company"));

        if (!defaultContactExists) {
            contactList.add(new Contact("Save_Me_Company", "7905280916"));
            contactAdapter.notifyDataSetChanged();
            saveContactsToPreferences();
        }
    }

    private void logoutUser() {
        mAuth.signOut();
        Toast.makeText(SettingsActivity.this, "Logged out successfully.", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}
