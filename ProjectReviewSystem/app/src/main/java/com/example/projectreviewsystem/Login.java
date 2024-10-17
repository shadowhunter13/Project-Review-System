package com.example.projectreviewsystem;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class Login extends AppCompatActivity {

    private static final String TAG = "LoginActivity";  // Tag for logging

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);  // Load the admin layout

        // Find views by ID
        ImageView adminImage = findViewById(R.id.admin_image);  // Updated to use ImageView
        ImageView researcherImage = findViewById(R.id.researcher_image);  // Updated to use ImageView

        // Set OnClickListener for Admin ImageView
        adminImage.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Login.this, LoginAdmin.class);
                intent.putExtra("role", "Admin");  // Optional: Pass role information
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting Admin activity: ", e);
                Toast.makeText(Login.this, "Failed to start Admin activity.", Toast.LENGTH_SHORT).show();  // Display toast on error
            }
        });

        // Set OnClickListener for Researcher ImageView
        researcherImage.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Login.this, LoginResearcher.class);
                intent.putExtra("role", "Researcher");  // Optional: Pass role information
                startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Error starting Researcher activity: ", e);
                Toast.makeText(Login.this, "Failed to start Researcher activity.", Toast.LENGTH_SHORT).show();  // Display toast on error
            }
        });
    }
}
