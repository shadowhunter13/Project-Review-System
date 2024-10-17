package com.example.projectreviewsystem;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class splash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash); // Use the XML layout file

        // Delay for 5 seconds (5000 milliseconds)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Start the main activity after the delay
                Intent intent = new Intent(splash.this, Login.class);
                startActivity(intent);
                finish(); // Finish the splash activity
            }
        }, 2000); // 5000 milliseconds delay
    }
}
