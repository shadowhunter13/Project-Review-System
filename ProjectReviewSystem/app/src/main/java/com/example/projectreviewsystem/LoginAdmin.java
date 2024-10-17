package com.example.projectreviewsystem;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser ;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LoginAdmin extends AppCompatActivity {

    private GoogleSignInClient googleSignInClient;
    private static final int RC_SIGN_IN = 100;

    private EditText emailEditText;
    private EditText passwordEditText;
    private FirebaseAuth firebaseAuth;
    private DatabaseReference databaseReference;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loginadmin);

        // Initialize Firebase Auth and Database
        firebaseAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference("Users");

        // Initialize UI components
        emailEditText = findViewById(R.id.email_id);
        passwordEditText = findViewById(R.id.pass_word);
        Button loginButton1 = findViewById(R.id.login_button);
        EditText googleSignInButton1 = findViewById(R.id.go_ogle);
        TextView forgotPassword1 = findViewById(R.id.forgot_password);

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // Handle Login Button Click
        loginButton1.setOnClickListener(v -> handleLogin());

        // Handle Google Sign-In Click
        googleSignInButton1.setOnClickListener(v -> startGoogleSignIn());

        // Handle Forgot Password Click
        forgotPassword1.setOnClickListener(v -> openEmailApp());

        // Check if user is already logged in
        checkIfUserLoggedIn();
    }

    private void handleLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate email format
        if (!isValidEmail(email)) {
            Snackbar.make(findViewById(android.R.id.content), "Please enter a valid email", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Validate password length
        if (password.length() < 6) {
            Snackbar.make(findViewById(android.R.id.content), "Password must be at least 6 characters", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Show logging information
        Log.i("Login", "Email: " + email);
        Log.i("Login", "Password: ******"); // Use asterisks to hide the password

        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser  user = firebaseAuth.getCurrentUser();
                        saveUserToDatabase(user);
                        showSnackbar("Login Successful", true);
                        redirectToMainScreen();
                    } else {
                        // Show logging information for the exception
                        Log.e("Login", "Login Failed: " + task.getException().getMessage());
                        showSnackbar("Login Failed: " + task.getException().getMessage(), false);
                    }
                });
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
            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
            firebaseAuth.signInWithCredential(credential)
                    .addOnCompleteListener(this, task1 -> {
                        if (task1.isSuccessful()) {
                            FirebaseUser  user = firebaseAuth.getCurrentUser();
                            saveUserToDatabase(user);
                            showSnackbar("Google Sign-In Successful", true);
                            redirectToMainScreen();
                        } else {
                            // Show logging information for the exception
                            Log.e("Google Sign-In", "Google Sign-In Failed: " +
                                    task1.getException().getMessage());
                            showSnackbar("Google Sign-In Failed: " + task1.getException().getMessage(), false);
                        }
                    });
        } catch (Exception e) {
            // Show logging information for the exception
            Log.e("Google Sign-In", "Google Sign-In Failed: " + e.getMessage());
            showSnackbar("Google Sign-In Failed", false);
        }
    }

    private void saveUserToDatabase(FirebaseUser  user) {
        if (user != null) {
            String userId = user.getUid();
            databaseReference.child(userId).setValue(user.getEmail());
        }
    }

    private void openEmailApp() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Reset Password");
        startActivity(Intent.createChooser(intent, "Open Email"));
    }

    private void checkIfUserLoggedIn() {
        FirebaseUser  user = firebaseAuth.getCurrentUser();
        if (user != null) {
            redirectToMainScreen();
        }
    }

    private void redirectToMainScreen() {
        Intent intent = new Intent(LoginAdmin.this, Admin.class);
        startActivity(intent);
        finish();
    }

    private void showSnackbar(String message, boolean isSuccess) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(isSuccess ? 0xFF4CAF50 : 0xFFF44336);  // Green for success, red for failure
        snackbar.show();
    }

    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }
}