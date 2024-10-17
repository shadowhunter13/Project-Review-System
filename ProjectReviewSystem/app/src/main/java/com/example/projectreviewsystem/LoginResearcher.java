package com.example.projectreviewsystem;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginResearcher extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100;

    private EditText emailEditText, passwordEditText;
    private Button registerButton;  // Change to registerButton
    private TextView googleSignIn, forgotPassword;

    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_research);  // Load the admin layout

        // Initialize FirebaseAuth and GoogleSignInClient
        firebaseAuth = FirebaseAuth.getInstance();
        configureGoogleSignIn();

        // Initialize UI components
        emailEditText = findViewById(R.id.emailid);
        passwordEditText = findViewById(R.id.password);
        registerButton = findViewById(R.id.loginbutton);  // Changed to register button
        googleSignIn = findViewById(R.id.google);
        forgotPassword = findViewById(R.id.forgotpassword);

        // Handle Register Button Click
        registerButton.setOnClickListener(v -> registerUser());  // Call registerUser on click

        // Handle Google Sign-In Click
        googleSignIn.setOnClickListener(v -> startGoogleSignIn());

        // Handle Forgot Password
        forgotPassword.setOnClickListener(v -> openEmailApp());
        emailEditText.setTextColor(getResources().getColor(android.R.color.white));
        passwordEditText.setTextColor(getResources().getColor(android.R.color.white));
    }

    private void configureGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

        private void registerUser() {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString().trim();

            // Validate email format
            if (!isValidEmail(email)) {
                Snackbar.make(findViewById(R.id.main), "Please enter a valid email", Snackbar.LENGTH_SHORT).show();
                return;
            }

            // Validate password length
            if (password.length() < 6) {
                Snackbar.make(findViewById(R.id.main), "Password must be at least 6 characters", Snackbar.LENGTH_SHORT).show();
                return;
            }

            // Show logging information
            Log.i("Registration", "Email: " + email);
            Log.i("Registration", "Password: ******"); // Use asterisks to hide the password

            firebaseAuth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            showSnackbar("Registration Successful", true);
                            redirectToMainScreen();
                        } else {
                            // Show logging information for the exception
                            Log.e("Registration", "Registration Failed: " + task.getException().getMessage());
                            showSnackbar("Registration Failed: " + task.getException().getMessage(), false);
                        }
                    });
        }

        private boolean isValidEmail(String email) {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
        }
    private void startGoogleSignIn() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleGoogleSignInResult(task);
        }
    }

    private void handleGoogleSignInResult(Task<GoogleSignInAccount> task) {
        try {
            GoogleSignInAccount account = task.getResult();
            firebaseAuthWithGoogle(account);
        } catch (Exception e) {
            showSnackbar("Google Sign-In Failed: " + e.getMessage(), false);
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        firebaseAuth.signInWithCredential(GoogleAuthProvider.getCredential(account.getIdToken(), null))
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        showSnackbar("Google Sign-In Successful", true);
                        redirectToMainScreen();
                    } else {
                        showSnackbar("Google Sign-In Failed: " + task.getException().getMessage(), false);
                    }
                });
    }

    private void openEmailApp() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Reset Password");
        startActivity(Intent.createChooser(intent, "Open Email"));
    }

    private void redirectToMainScreen() {
        Intent intent = new Intent(LoginResearcher.this, Faculty.class);
        startActivity(intent);
        finish();  // Close Login Activity
    }

    private void showSnackbar(String message, boolean isSuccess) {
        Snackbar snackbar = Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(isSuccess ? getColor(android.R.color.holo_green_light) : getColor(android.R.color.holo_red_light));
        snackbar.show();
    }
}
