package com.example.projectreviewsystem;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser ;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class Designation extends AppCompatActivity {

    private EditText designationEditText;
    private EditText departmentEditText;
    private Button saveButton;

    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference; // Reference to the Realtime Database

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_designation);

        // Initialize Firebase Auth and Database
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("researchers"); // Reference to the "researchers" node

        // Get the user's data from the Intent
        String userId = getIntent().getStringExtra("USER_ID");
        String userName = getIntent().getStringExtra("USER_NAME");
        String email = getIntent().getStringExtra("EMAIL"); // Retrieve the email
        HashMap<String, Object> projects = (HashMap<String, Object>) getIntent().getSerializableExtra("PROJECTS");
        String profilePhotoUrl = getIntent().getStringExtra("profile_photo");

        // Initialize UI elements
        designationEditText = findViewById(R.id.designation);
        departmentEditText = findViewById(R.id.department);
        saveButton = findViewById(R.id.save_button);

        // Set click listener for the save button
        saveButton.setOnClickListener(v -> saveDesignationAndDepartment(userId, userName, email, projects,profilePhotoUrl));
    }

    private void saveDesignationAndDepartment(String userId, String userName, String email, HashMap<String, Object> projects,String profilePhotoUrl) {
        String designation = designationEditText.getText().toString().trim();
        String department = departmentEditText.getText().toString().trim();

        // Validate input
        if (designation.isEmpty() || department.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create a map to store the designation, department, name, email, and projects
        Map<String, Object> designationData = new HashMap<>();
        designationData.put("designation", designation);
        designationData.put("department", department);
        designationData.put("email", email); // Store the email
        designationData.put("name", userName); // Store the user's name
        designationData.put("profile_photo",profilePhotoUrl);
        designationData.put("projects", projects); // Store the projects
        String[] uniqueId = userName.split(" ");

        // Save to Realtime Database in the "researchers" node using userId as the document ID
        databaseReference.child(uniqueId[0]).setValue(designationData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Designation.this, "Data saved successfully", Toast.LENGTH_SHORT).show();
                    redirectToActivity(Faculty.class,userName); // Redirect to Faculty after saving
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Designation.this, "Error saving data: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("Designation", "Error saving data: ", e);
                });
    }

//
    private void redirectToActivity(Class<?> activityClass,String userName) {
        Intent intent = new Intent(Designation.this, activityClass);
        intent.putExtra("USER_NAME", userName);
        startActivity(intent);
        finish();
    }
}