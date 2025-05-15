package com.example.moneyhays;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LogHistoryActivity extends AppCompatActivity {

    private static final String TAG = "LogHistoryActivity";
    private TextView logHistoryTextView;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference logsRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_history);

        logHistoryTextView = findViewById(R.id.logHistoryTextView);
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            logsRef = FirebaseDatabase.getInstance().getReference("logs").child(currentUser.getUid());
            getLogHistory();
        } else {
            logHistoryTextView.setText("No user is currently logged in.");
        }
    }

    private void getLogHistory() {
        logsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    logHistoryTextView.setText("No logs found.");
                    return;
                }

                StringBuilder historyText = new StringBuilder();

                for (DataSnapshot logSnapshot : snapshot.getChildren()) {
                    String timestamp = logSnapshot.getKey();

                    Long onePesoCount = logSnapshot.child("onePesoCount").getValue(Long.class);
                    Long fivePesoCount = logSnapshot.child("fivePesoCount").getValue(Long.class);
                    Long tenPesoCount = logSnapshot.child("tenPesoCount").getValue(Long.class);
                    Long twentyPesoCount = logSnapshot.child("twentyPesoCount").getValue(Long.class);
                    Long totalAmount = logSnapshot.child("totalAmount").getValue(Long.class);

                    onePesoCount = onePesoCount != null ? onePesoCount : 0;
                    fivePesoCount = fivePesoCount != null ? fivePesoCount : 0;
                    tenPesoCount = tenPesoCount != null ? tenPesoCount : 0;
                    twentyPesoCount = twentyPesoCount != null ? twentyPesoCount : 0;
                    totalAmount = totalAmount != null ? totalAmount : 0;

                    historyText.append("🕒 Timestamp: ").append(timestamp).append("\n");
                    historyText.append("₱1: ").append(onePesoCount).append(" pcs - ₱")
                            .append(onePesoCount * 1).append("\n");
                    historyText.append("₱5: ").append(fivePesoCount).append(" pcs - ₱")
                            .append(fivePesoCount * 5).append("\n");
                    historyText.append("₱10: ").append(tenPesoCount).append(" pcs - ₱")
                            .append(tenPesoCount * 10).append("\n");
                    historyText.append("₱20: ").append(twentyPesoCount).append(" pcs - ₱")
                            .append(twentyPesoCount * 20).append("\n");
                    historyText.append("💰 Total: ₱").append(totalAmount).append("\n\n");
                }

                logHistoryTextView.setText(historyText.toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error retrieving log history", error.toException());
                logHistoryTextView.setText("Error retrieving log history.");
            }
        });
    }
}
