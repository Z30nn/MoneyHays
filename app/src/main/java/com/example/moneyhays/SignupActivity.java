package com.example.moneyhays;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {

    private EditText usernameEditText;
    private EditText emailEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private Button signUpButton;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private static final String TAG = "SignupActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Initialize views
        usernameEditText = findViewById(R.id.usernameEditText);
        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        signUpButton = findViewById(R.id.signUpButton);
        progressBar = findViewById(R.id.progressBar); // Added missing progressBar initialization
        TextView loginTextView = findViewById(R.id.loginTextView);

        // Sign up button click listener
        signUpButton.setOnClickListener(v -> {
            registerUser();
        });

        // Navigate to Login Activity
        loginTextView.setOnClickListener(v -> {
            Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
            startActivity(intent);
            finish(); // Close SignupActivity
        });
    }

    private void registerUser() {
        // Get input values
        String username = usernameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString();
        String confirmPassword = confirmPasswordEditText.getText().toString();

        // Validate inputs
        if (username.isEmpty()) {
            usernameEditText.setError("Username is required");
            usernameEditText.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailEditText.setError("Please enter a valid email");
            emailEditText.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            passwordEditText.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match");
            confirmPasswordEditText.requestFocus();
            return;
        }

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        signUpButton.setEnabled(false);

        // Create user with Firebase
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");

                        // Set display name for the user
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(username)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            Log.d(TAG, "User profile updated.");
                                            // Create user document in Firestore
                                            createUserInFirestore(user.getUid(), username);
                                        } else {
                                            // Profile update failed, but user was created
                                            createUserInFirestore(user.getUid(), username);
                                        }
                                    });
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        signUpButton.setEnabled(true);

                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() :
                                "Registration failed";

                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(SignupActivity.this,
                                "Registration failed: " + errorMessage,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void createUserInFirestore(String userId, String username) {
        // Create a new user with only name and status
        Map<String, Object> user = new HashMap<>();
        user.put("name", username); // Change "username" to "name"
        user.put("status", 0); // Default status to 0 (not logged in)

        // Add a new document to "users" collection with user ID as the document ID
        db.collection("users").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User document added to Firestore");
                    // Sign out to ensure clean login flow
                    mAuth.signOut();
                    // Ensure UI updates happen on the main thread
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        signUpButton.setEnabled(true);
                        // Inform user and redirect to login
                        Toast.makeText(SignupActivity.this,
                                "Account created successfully! Please log in.",
                                Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    });
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error adding user document", e);
                    // Ensure UI updates happen on the main thread
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        signUpButton.setEnabled(true);
                        Toast.makeText(SignupActivity.this,
                                "Account created but profile setup failed. Please login anyway.",
                                Toast.LENGTH_LONG).show();
                        // Sign out to ensure clean login flow
                        mAuth.signOut();
                        Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    });
                });
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Navigate to Login Activity when back button is pressed
        Intent intent = new Intent(SignupActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}