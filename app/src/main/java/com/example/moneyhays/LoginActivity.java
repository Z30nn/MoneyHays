package com.example.moneyhays;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private ProgressBar progressBar;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private static final String TAG = "LoginActivity";

    // SharedPreferences keys
    private static final String PREFS_NAME = "UserStatusPrefs";
    private static final String USER_STATUS_KEY = "user_status";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize Firebase Realtime Database
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize views
        emailEditText = findViewById(R.id.loginEmailEditText);
        passwordEditText = findViewById(R.id.loginPasswordEditText);
        loginButton = findViewById(R.id.loginButton);
        progressBar = findViewById(R.id.progressBar);
        TextView signupTextView = findViewById(R.id.signupTextView);
        TextView forgotPasswordTextView = findViewById(R.id.forgotPasswordTextView);

        // Login button click
        loginButton.setOnClickListener(v -> loginUser());

        // Navigate to SignupActivity
        signupTextView.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });

        // Navigate to ForgotPasswordActivity
        forgotPasswordTextView.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return;
        }

        // Show progress
        progressBar.setVisibility(View.VISIBLE);
        loginButton.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Check if user has a record in Realtime Database
                            checkAndCreateUserRecord(user);
                        } else {
                            // This case should ideally not happen if task.isSuccessful() is true
                            progressBar.setVisibility(View.GONE);
                            loginButton.setEnabled(true);
                            Toast.makeText(LoginActivity.this, "Login failed: User not found after successful authentication.", Toast.LENGTH_LONG).show();
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        loginButton.setEnabled(true);
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Authentication failed";
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(LoginActivity.this, "Authentication failed: " + errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkAndCreateUserRecord(FirebaseUser user) {
        DatabaseReference userRef = mDatabase.child("users").child(user.getUid());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // User record exists, proceed with login
                    completeLogin(user);
                } else {
                    // User record doesn't exist, create it
                    createUserRecord(user);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Error checking user record, proceed with login anyway
                Log.w(TAG, "Error checking user record: ", databaseError.toException());
                completeLogin(user);
            }
        });
    }

    private void createUserRecord(FirebaseUser  user) {
        // Create a new user record with only name and status
        Map<String, Object> userData = new HashMap<>();
        userData.put("name", user.getDisplayName() != null ? user.getDisplayName() : "User "); // Change "username" to "name"
        userData.put("status", 1); // Set status to 1 (logged in)

        mDatabase.child("users").child(user.getUid())
                .setValue(userData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User record created successfully");
                    completeLogin(user);
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Error creating user record", e);
                    // Proceed with login anyway
                    completeLogin(user);
                });
    }


    private void completeLogin(FirebaseUser user) {
        // Update user status to 1 (logged in) in the database
        mDatabase.child("users").child(user.getUid()).child("status").setValue(1);

        // Set current user as active user
        mDatabase.child("users").child("activeUser").setValue(user.getUid());

        // Record login in logs
        recordLoginActivity(user.getUid(), user.getEmail());

        // Set user status to 1 (logged in) in SharedPreferences
        setUserStatus(1);

        progressBar.setVisibility(View.GONE);
        loginButton.setEnabled(true);

        Toast.makeText(LoginActivity.this, "Login successful!", Toast.LENGTH_SHORT).show();

        // Redirect to MainActivity
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish(); // Finish LoginActivity so user can't go back
    }

    private void recordLoginActivity(String userId, String email) {
        // Get current time
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        String currentDateTime = sdf.format(new Date());

        // Create log entry
        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("userId", userId);
        logEntry.put("email", email);
        logEntry.put("action", "login");
        logEntry.put("status", 1); // Status is 1 for login
        logEntry.put("timestamp", currentDateTime);
        logEntry.put("deviceInfo", android.os.Build.MODEL);

        // Add to Realtime Database
        String logKey = mDatabase.child("user_logs").push().getKey();
        if (logKey != null) {
            mDatabase.child("user_logs").child(logKey)
                    .setValue(logEntry)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Login activity logged with key: " + logKey);
                        // Add log reference to user's log history
                        mDatabase.child("users").child(userId).child("logHistory").child(logKey).setValue(true);
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Error adding login log", e);
                    });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is already logged in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User is already logged in");
            // Check if the user status is also 1 in SharedPreferences
            if (getUserStatus() == 1) {
                // Verify status in database
                mDatabase.child("users").child(currentUser.getUid()).child("status")
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists() && dataSnapshot.getValue(Integer.class) == 1) {
                                    // User is confirmed logged in in both SharedPreferences and database

                                    // Ensure activeUser is set
                                    mDatabase.child("users").child("activeUser").setValue(currentUser.getUid());

                                    // Redirect to MainActivity
                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finish(); // Finish LoginActivity
                                } else {
                                    // Status mismatch, update database
                                    mDatabase.child("users").child(currentUser.getUid()).child("status").setValue(1);

                                    // Set activeUser
                                    mDatabase.child("users").child("activeUser").setValue(currentUser.getUid());

                                    // Redirect to MainActivity
                                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                    finish(); // Finish LoginActivity
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                Log.w(TAG, "Error checking user status in database", databaseError.toException());
                                // Proceed to MainActivity anyway
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish(); // Finish LoginActivity
                            }
                        });
            } else {
                // If Firebase reports a logged-in user but SharedPreferences status is 0,
                // it might indicate a mismatch or an incomplete logout.
                Log.d(TAG, "Firebase user logged in, but SharedPreferences status is 0.");
                // Force logout to maintain consistency
                mAuth.signOut();
                // Update status in database to 0
                mDatabase.child("users").child(currentUser.getUid()).child("status").setValue(0);
                // Remove activeUser reference
                mDatabase.child("users").child("activeUser").removeValue();
            }
        }
    }

    // Method to set user status in SharedPreferences
    public static void setUserStatus(int status, Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(USER_STATUS_KEY, status);
        editor.apply();
    }

    // Overloaded method for convenience within the activity
    private void setUserStatus(int status) {
        setUserStatus(status, this);
    }

    // Method to get user status from SharedPreferences
    public static int getUserStatus(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getInt(USER_STATUS_KEY, 0); // Default is 0 (logged out)
    }

    // Overloaded method for convenience within the activity
    private int getUserStatus() {
        return getUserStatus(this);
    }

    @Override
    public void onBackPressed() {
        // Handle back button press to exit the app
        moveTaskToBack(true);
    }
}