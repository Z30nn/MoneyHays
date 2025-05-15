package com.example.moneyhays;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private static final String TAG = "ForgotPasswordActivity";
    private FirebaseAuth mAuth;
    private EditText emailEditText;
    private Button resetPasswordButton;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize views
        emailEditText = findViewById(R.id.reset_email_edit_text);
        resetPasswordButton = findViewById(R.id.reset_password_button);
        ImageButton backButton = findViewById(R.id.back_button);
        progressBar = findViewById(R.id.progressBar); // Make sure this is in your layout

        // Back button click listener
        backButton.setOnClickListener(v -> {
            finish(); // Just go back to previous activity
        });

        // Reset password button click listener
        resetPasswordButton.setOnClickListener(v -> resetPassword());
    }

    private void resetPassword() {
        String email = emailEditText.getText().toString().trim();

        // Validate email
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

        // Show progress and disable button
        progressBar.setVisibility(View.VISIBLE);
        resetPasswordButton.setEnabled(false);

        // Send password reset email
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    resetPasswordButton.setEnabled(true);

                    if (task.isSuccessful()) {
                        Log.d(TAG, "Password reset email sent to " + email);
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Password reset instructions sent to your email",
                                Toast.LENGTH_LONG).show();

                        // Return to login screen after a short delay
                        new android.os.Handler().postDelayed(() -> {
                            Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }, 1500); // 1.5 second delay to allow user to read the toast
                    } else {
                        String errorMessage = task.getException() != null ?
                                task.getException().getMessage() :
                                "Failed to send reset email";
                        Log.w(TAG, "Failed to send password reset email", task.getException());
                        Toast.makeText(ForgotPasswordActivity.this,
                                "Error: " + errorMessage,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        // Return to login screen
        Intent intent = new Intent(ForgotPasswordActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }
}