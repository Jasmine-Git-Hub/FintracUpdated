package com.example.fintrac;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class ActivityLogActivity extends AppCompatActivity {
    private static final String TAG = "ActivityLogActivity";
    private TextView titleView;
    private TextView messageView;
    private JarFirestoreManager jarFirestoreManager;
    private String jarId;
    private String jarName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SettingHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_activity_log);

        jarFirestoreManager = new JarFirestoreManager();

        // Get Data from Intent
        jarId = getIntent().getStringExtra("JAR_ID");
        jarName = getIntent().getStringExtra("JAR_NAME");

        initializeViews();
        setupBackButton();

        if (jarId != null && !jarId.isEmpty()) {
            loadActivityLog();
        } else {
            showEmptyState();
        }
    }

    private void initializeViews() {
        titleView = findViewById(R.id.title_text);
        messageView = findViewById(R.id.message);

        if (titleView != null) {
            if (jarName != null && !jarName.isEmpty()) {
                titleView.setText(jarName + " - Activity Log");
            } else {
                titleView.setText("Activity Log");
            }
        }
    }

    private void setupBackButton() {
        ImageButton backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }
    }

    private void loadActivityLog() {
        jarFirestoreManager.getTransactions(jarId, new JarFirestoreManager.FirestoreCallback<List<JarTransaction>>() {
            @Override
            public void onSuccess(List<JarTransaction> transactions) {
                runOnUiThread(() -> displayTransactions(transactions));
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Error loading transactions", e);
                runOnUiThread(() -> {
                    messageView.setText("Error loading activity log: " + e.getMessage());
                });
            }
        });
    }

    private void displayTransactions(List<JarTransaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            showEmptyState();
            return;
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault());
        StringBuilder sb = new StringBuilder();

        for (JarTransaction transaction : transactions) {
            sb.append("• ");

            // Get user name (from transaction or use fallback)
            String userName = transaction.getUserName();
            if (userName == null || userName.isEmpty()) {
                userName = "Someone";
            }

            // Format based on transaction type
            String type = transaction.getType();
            double amount = transaction.getAmount();
            String note = transaction.getNote();

            if ("CREATE".equals(type)) {
                sb.append(userName).append(" created this jar");
                if (amount > 0) {
                    sb.append(" with initial amount of ").append(formatCurrency(amount));
                }
            } else if ("ADD_MONEY".equals(type)) {
                sb.append(userName).append(" added ").append(formatCurrency(amount));
                if (note != null && !note.isEmpty() && !note.equals("Added money to jar")) {
                    sb.append(" - ").append(note);
                }
            } else if ("ADD_PARTICIPANT".equals(type)) {
                sb.append(userName).append(" ").append(note != null ? note : "joined the jar");
            } else {
                sb.append(userName).append(": ").append(note != null ? note : "No description");
            }

            // Add timestamp
            if (transaction.getTimestamp() != null) {
                sb.append(" (").append(dateFormat.format(transaction.getTimestamp().toDate())).append(")");
            }

            sb.append("\n\n");
        }

        messageView.setText(sb.toString());
    }

    private String formatCurrency(double amount) {
        return SettingHelper.formatAmount(this, amount);
    }

    private void showEmptyState() {
        messageView.setText("No activity recorded yet.\n\n" +
                "Add money to your jar or invite participants to see activity here.");
    }
}