package com.example.projectreviewsystem;

import static android.provider.Settings.System.getString;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class LoginResearcher extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100;
    private EditText emailEditText, passwordEditText;
    private Button registerButton;
    private TextView googleSignIn, forgotPassword;

    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_research);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        configureGoogleSignIn();

        emailEditText = findViewById(R.id.emailid);
        passwordEditText = findViewById(R.id.password);
        registerButton = findViewById(R.id.login_button);
        googleSignIn = findViewById(R.id.google);
        forgotPassword = findViewById(R.id.forgotpassword);

        registerButton.setOnClickListener(v -> registerUser ());
        googleSignIn.setOnClickListener(v -> startGoogleSignIn());
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

    private void registerUser () {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Snackbar.make(findViewById(R.id.main), "Please enter a valid email", Snackbar.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Snackbar.make(findViewById(R.id.main), "Password must be at least 6 characters", Snackbar.LENGTH_SHORT).show();
            return;
        }

        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
            if (task.isSuccessful() && firebaseAuth.getCurrentUser () != null) {
                saveUserDataToFirestore(firebaseAuth.getCurrentUser ().getUid(), email, null);
            } else {
                showSnackbar("Registration Failed: " + (task.getException() != null ? task.getException().getMessage() : ""), false);
            }
        });
    }

    private void startGoogleSignIn() {
        // Sign out the user to clear cached credentials
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
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
                    if (task.isSuccessful() && firebaseAuth.getCurrentUser () != null) {
                        checkIfUserExists(firebaseAuth.getCurrentUser ().getUid(), account.getEmail(), account.getPhotoUrl());
                    } else {
                        showSnackbar("Google Sign-In Failed: " + (task.getException() != null ? task.getException().getMessage() : ""), false);
                    }
                });
    }

    private void checkIfUserExists(String userId, String email, Uri photoUri) {
        DocumentReference docRef = firestore.collection("faculty").document(userId);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // User exists, redirect to Faculty
                redirectToActivity(Faculty.class);
            } else {
                saveUserDataToFirestore(userId, email, photoUri);
                redirectToActivity(Designation.class);
            }
        }).addOnFailureListener(e -> Log.e("Firestore", "Failed to check user existence: " + e.getMessage()));
    }

    private void saveUserDataToFirestore(String userId, String email, Uri photoUri) {
        DocumentReference docRef = firestore.collection("faculty").document(userId);

        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);
        userData.put("name", firebaseAuth.getCurrentUser ().getDisplayName());
        if (photoUri != null) {
            userData.put("profile_photo", photoUri.toString());
        }

        docRef.set(userData)
                .addOnSuccessListener(aVoid -> Log.i("Firestore", "User  data saved successfully"))
                .addOnFailureListener(e -> Log.e("Firestore", "Failed to save user data", e));
    }

    private void redirectToActivity(Class<?> activityClass) {
        Intent intent = new Intent(LoginResearcher.this, Faculty.class);
        startActivity(intent);
        finish();
    }

    private void openEmailApp() {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Reset Password");
        startActivity(Intent.createChooser(intent, "Open Email"));
    }

    private void showSnackbar(String message, boolean isSuccess) {
        Snackbar snackbar = Snackbar.make(findViewById(R.id.main), message, Snackbar.LENGTH_LONG);
        snackbar.setBackgroundTint(isSuccess ? getColor(android.R.color.holo_green_light) : getColor(android.R.color.holo_red_light));
        snackbar.show();
    }
}