// MainActivity.java
package com.example.moneyhays;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DatabaseError;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private ImageView fivePesoImage;
    private ImageView tenPesoImage;
    private ImageView twentyPesoImage;
    private ImageView statusIcon;
    private TextView onePesoCount, fivePesoCount, tenPesoCount, twentyPesoCount;
    private TextView onePesoTotal, fivePesoTotal, tenPesoTotal, twentyPesoTotal;
    private TextView totalAmount;
    private Button resetButton, logoutButton, historyButton;


    // Firebase
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private FirebaseUser currentUser; // Keep track of the current user

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize UI elements
        // UI Elements
        ImageView onePesoImage = findViewById(R.id.onePesoImage);
        fivePesoImage = findViewById(R.id.fivePesoImage);
        tenPesoImage = findViewById(R.id.tenPesoImage);
        twentyPesoImage = findViewById(R.id.twentyPesoImage);
        onePesoCount = findViewById(R.id.onePesoCount);
        fivePesoCount = findViewById(R.id.fivePesoCount);
        tenPesoCount = findViewById(R.id.tenPesoCount);
        twentyPesoCount = findViewById(R.id.twentyPesoCount);
        onePesoTotal = findViewById(R.id.onePesoTotal);
        fivePesoTotal = findViewById(R.id.fivePesoTotal);
        tenPesoTotal = findViewById(R.id.tenPesoTotal);
        twentyPesoTotal = findViewById(R.id.twentyPesoTotal);
        totalAmount = findViewById(R.id.totalAmount);
        logoutButton = findViewById(R.id.logoutButton);
        historyButton = findViewById(R.id.historyButton);
        resetButton = findViewById(R.id.resetButton);

        // revert later
        // mDatabase.child("users").child("activeUser").setValue(currentUser.getUid());

        // Check for saved instance state (optional, for handling orientation changes)
        if (savedInstanceState != null) {
            // Restore UI state here if needed
        }

        // Logout button click listener
        logoutButton.setOnClickListener(v -> {
            storeDetailedLog("Logout");
            resetCountsAndTotal();

            // Record logout activity first
            if (currentUser != null) {
                // Update user status to 0 (logged out) in Realtime Database
                mDatabase.child("users").child(currentUser.getUid()).child("status").setValue(0);
                // Remove the active user
                mDatabase.child("users").child("activeUser").removeValue();
            }

            // Sign out the user
            mAuth.signOut();

            // Set user status to 0 (logged out) in SharedPreferences
            LoginActivity.setUserStatus(0, this);

            currentUser = null; // Clear the current user
            Toast.makeText(MainActivity.this, "Logged Out!", Toast.LENGTH_SHORT).show();

            // Go back to Login Activity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        resetButton.setOnClickListener(v -> {
            storeDetailedLog("Reset");
            resetCountsAndTotal();
        });

        // Set click listener for history button
        historyButton.setOnClickListener(v -> {
            // Start the LogHistoryActivity
            Intent intent = new Intent(MainActivity.this, LogHistoryActivity.class);
            startActivity(intent);
        });

        // Initialize the current user
        currentUser = mAuth.getCurrentUser();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check if user is signed in
        currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // If not signed in, go to Login Activity
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        } else {
            // Check the user status
            if (LoginActivity.getUserStatus(this) == 0) {
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        }

        updateCountsAndTotal();
    }

    // Member variable to hold the ValueEventListener
    private ValueEventListener coinDataListener;

    // Method to update coin counts and total
    public void updateCountsAndTotal() {
        // Get a reference to the Firebase Realtime Database for the current user
        if (currentUser  != null) {
            DatabaseReference userCoinsRef = mDatabase.child("users").child(currentUser .getUid()).child("coinSorter");

            // Set up a ValueEventListener to listen for changes
            coinDataListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    // Check if data exists
                    if (dataSnapshot.exists()) {
                        // Retrieve coin counts from the database
                        Integer onePesoCountVal = dataSnapshot.child("peso1").getValue(Integer.class);
                        Integer fivePesoCountVal = dataSnapshot.child("peso5").getValue(Integer.class);
                        Integer tenPesoCountVal = dataSnapshot.child("peso10").getValue(Integer.class);
                        Integer twentyPesoCountVal = dataSnapshot.child("peso20").getValue(Integer.class);

                        // Calculate totals
                        int onePesoTotalVal = onePesoCountVal != null ? onePesoCountVal * 1 : 0;
                        int fivePesoTotalVal = fivePesoCountVal != null ? fivePesoCountVal * 5 : 0;
                        int tenPesoTotalVal = tenPesoCountVal != null ? tenPesoCountVal * 10 : 0;
                        int twentyPesoTotalVal = twentyPesoCountVal != null ? twentyPesoCountVal * 20 : 0;
                        int totalAmountVal = onePesoTotalVal + fivePesoTotalVal + tenPesoTotalVal + twentyPesoTotalVal;

                        // Update the UI
                        runOnUiThread(() -> {
                            onePesoCount.setText(getString(R.string.peso_count_format, onePesoCountVal));
                            fivePesoCount.setText(getString(R.string.peso_count_format, fivePesoCountVal));
                            tenPesoCount.setText(getString(R.string.peso_count_format, tenPesoCountVal));
                            twentyPesoCount.setText(getString(R.string.peso_count_format, twentyPesoCountVal));

                            onePesoTotal.setText(getString(R.string.peso_total_format, onePesoTotalVal));
                            fivePesoTotal.setText(getString(R.string.peso_total_format, fivePesoTotalVal));
                            tenPesoTotal.setText(getString(R.string.peso_total_format, tenPesoTotalVal));
                            twentyPesoTotal.setText(getString(R.string.peso_total_format, twentyPesoTotalVal));
                            totalAmount.setText(getString(R.string.peso_total_format, totalAmountVal));
                        });
                    } else {
                        // Handle the case where data does not exist
                        runOnUiThread(() -> {
                            onePesoTotal.setText(getString(R.string.peso_zero));
                            fivePesoTotal.setText(getString(R.string.peso_zero));
                            tenPesoTotal.setText(getString(R.string.peso_zero));
                            twentyPesoTotal.setText(getString(R.string.peso_zero));
                            totalAmount.setText(getString(R.string.peso_zero));
                        });
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    // Handle possible errors
                    Log.e(TAG, "Error retrieving data", databaseError.toException());
                }
            };

            // Attach the listener to the database reference
            userCoinsRef.addValueEventListener(coinDataListener);
        } else {
            // Handle the case where the user is not logged in
            Toast.makeText(this, "User  not logged in", Toast.LENGTH_SHORT).show();
        }
    }

    private void storeDetailedLog(String action) {
        if (currentUser != null) {
            String userId = currentUser.getUid();
            String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss", Locale.getDefault()).format(new Date());

            int oneCount = parseCount(onePesoCount.getText().toString());
            int fiveCount = parseCount(fivePesoCount.getText().toString());
            int tenCount = parseCount(tenPesoCount.getText().toString());
            int twentyCount = parseCount(twentyPesoCount.getText().toString());

            int oneTotal = oneCount * 1;
            int fiveTotal = fiveCount * 5;
            int tenTotal = tenCount * 10;
            int twentyTotal = twentyCount * 20;
            int total = oneTotal + fiveTotal + tenTotal + twentyTotal;

            // Skip logging if all values are zero
            if (oneCount == 0 && fiveCount == 0 && tenCount == 0 && twentyCount == 0 && total == 0) {
                Log.d(TAG, "Log not stored: All coin counts and total amount are 0.");
                return;
            }

            Map<String, Object> detailedLog = new HashMap<>();
            detailedLog.put("onePesoCount", oneCount);
            detailedLog.put("fivePesoCount", fiveCount);
            detailedLog.put("tenPesoCount", tenCount);
            detailedLog.put("twentyPesoCount", twentyCount);
            detailedLog.put("onePesoTotal", oneTotal);
            detailedLog.put("fivePesoTotal", fiveTotal);
            detailedLog.put("tenPesoTotal", tenTotal);
            detailedLog.put("twentyPesoTotal", twentyTotal);
            detailedLog.put("totalAmount", total);
            detailedLog.put("action", action);

            mDatabase.child("logs").child(userId).child(timestamp).setValue(detailedLog)
                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Detailed " + action + " log stored at: logs/" + userId + "/" + timestamp))
                    .addOnFailureListener(e -> Log.e(TAG, "Error storing detailed " + action + " log", e));
        }
    }
    
    // Utility method to extract number from "x pcs"
    private int parseCount(String text) {
        try {
            return Integer.parseInt(text.replaceAll("\\D+", ""));
        } catch (Exception e) {
            return 0;
        }
    }


    // Method to reset coin counts and total amount
    private void resetCountsAndTotal() {
        onePesoCount.setText(getString(R.string.zero_pieces));
        fivePesoCount.setText(getString(R.string.zero_pieces));
        tenPesoCount.setText(getString(R.string.zero_pieces));
        twentyPesoCount.setText(getString(R.string.zero_pieces));
        onePesoTotal.setText(getString(R.string.peso_zero));
        fivePesoTotal.setText(getString(R.string.peso_zero));
        tenPesoTotal.setText(getString(R.string.peso_zero));
        twentyPesoTotal.setText(getString(R.string.peso_zero));
        totalAmount.setText(getString(R.string.peso_zero));

        if (currentUser != null) {
            DatabaseReference coinRef = mDatabase.child("users").child(currentUser.getUid()).child("coinSorter");
            Map<String, Object> resetMap = new HashMap<>();
            resetMap.put("peso1", 0);
            resetMap.put("peso5", 0);
            resetMap.put("peso10", 0);
            resetMap.put("peso20", 0);
            resetMap.put("total", 0);
            coinRef.updateChildren(resetMap);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check for user session
        currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // User is signed in. Check User status.
            if (LoginActivity.getUserStatus(this) == 0) {
                // User is logged in, but status is 0. Force a logout.
                mAuth.signOut();
                LoginActivity.setUserStatus(0, this);

                // Update status in database
                mDatabase.child("users").child(currentUser.getUid()).child("status").setValue(0);

                currentUser = null; // Update current user
                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
                Toast.makeText(this, "Please log in again.", Toast.LENGTH_SHORT).show();
            }
        } else {
            // No user signed in, go to login.
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }
}