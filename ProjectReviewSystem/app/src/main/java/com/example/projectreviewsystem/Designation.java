package com.example.projectreviewsystem;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Designation extends AppCompatActivity {

    private EditText designationEditText;
    private EditText departmentEditText;
    private Button saveButton;

    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_designation);

        // Initialize Firebase Auth and Firestore
        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize UI elements
        designationEditText = findViewById(R.id.designation);
        departmentEditText = findViewById(R.id.department);
        saveButton = findViewById(R.id.save_button);

        // Set click listener for the save button
        saveButton.setOnClickListener(v -> saveDesignationAndDepartment());
    }

    private void saveDesignationAndDepartment() {
        String designation = designationEditText.getText().toString().trim();
        String department = departmentEditText.getText().toString().trim();

        // Validate input
        if (designation.isEmpty() || department.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get the current user's ID
        String userId = firebaseAuth.getCurrentUser ().getUid();

        // Create a map to store the designation and department
        Map<String, Object> designationData = new HashMap<>();
        designationData.put("designation", designation);
        designationData.put("department", department);

        // Save to Firestore in the "designation" collection using userId as the document ID
        firestore.collection("designation").document(userId)
                .set(designationData) // Use set to create a new document
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(Designation.this, "Data saved successfully", Toast.LENGTH_SHORT).show();
                    finish(); // Close the activity after saving
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(Designation.this, "Error saving data", Toast.LENGTH_SHORT).show();
                    Log.e("Designation", "Error saving data: ", e);
                });
    }
}