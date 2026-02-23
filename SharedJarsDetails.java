package com.example.fintrac;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SharedJarsDetails extends AppCompatActivity {

    private String jarId;
    private String jarName;
    private double totalSaved;
    private double totalGoal;
    private String iconName;
    private int color;
    private String jarType;

    private double myContribution = 0.0;
    private final List<JarTransaction> transactionList = new ArrayList<>();
    private Map<String, String> userNamesCache = new HashMap<>(); // Cache for user names

    private TextView totalSavedView, totalGoalView, progressPercentage, jarTypeView;
    private TextView progressSuggestionText;
    private LinearProgressIndicator progressBar;
    private LinearLayout participantsContainer;
    private RecyclerView transactionsRecyclerView;
    private TransactionAdapter transactionAdapter;
    private MaterialButton viewActivityButton, inviteButton, addMoneyButton;
    private MaterialCardView jarTypeCard;

    private JarFirestoreManager firestoreManager;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SettingHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shared_jars_details);

        firestoreManager = new JarFirestoreManager();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        Intent intent = getIntent();
        if (intent != null) {
            jarId = intent.getStringExtra("JAR_ID");
            jarName = intent.getStringExtra("JAR_NAME");
            totalSaved = intent.getDoubleExtra("JAR_SAVED", 0.0);
            totalGoal = intent.getDoubleExtra("JAR_GOAL", 0.0);
            iconName = intent.getStringExtra("ICON_NAME");
            color = intent.getIntExtra("COLOR", 0xFFF44336);
            jarType = intent.getStringExtra("JAR_TYPE");
        }

        initViews();
        setupUI();
        setupClickListeners();

        loadJarDetails();
        loadTransactions();
    }

    private void initViews() {
        totalSavedView = findViewById(R.id.total_saved);
        totalGoalView = findViewById(R.id.total_goal);
        progressPercentage = findViewById(R.id.progress_percentage);
        progressBar = findViewById(R.id.progress_bar);
        participantsContainer = findViewById(R.id.participants_container);
        jarTypeView = findViewById(R.id.jar_type);
        viewActivityButton = findViewById(R.id.view_activity_button);
        inviteButton = findViewById(R.id.invite_button);
        addMoneyButton = findViewById(R.id.add_money_button);
        progressSuggestionText = findViewById(R.id.progress_suggestion_text);

        if (jarTypeView != null) {
            jarTypeCard = (MaterialCardView) jarTypeView.getParent();
        }

        if (participantsContainer != null) {
            participantsContainer.removeAllViews();
        }

        transactionsRecyclerView = findViewById(R.id.transactions_recycler_view);
        if (transactionsRecyclerView != null) {
            transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            transactionAdapter = new TransactionAdapter(transactionList);
            transactionsRecyclerView.setAdapter(transactionAdapter);
            transactionsRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void setupUI() {
        TextView jarTitle = findViewById(R.id.jar_title);
        ImageView jarIcon = findViewById(R.id.jar_icon);

        if (jarTitle != null) jarTitle.setText(jarName != null ? jarName : "Shared Jar");
        if (jarIcon != null) {
            jarIcon.setImageResource(getIconResourceId(iconName));
            jarIcon.setBackgroundTintList(ColorStateList.valueOf(color));
            jarIcon.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white)));
        }

        if (jarTypeView != null) {
            jarTypeView.setText(jarType != null ? jarType : "Shared");
        }

        updateMainDisplay();
    }

    private void loadJarDetails() {
        if (jarId == null) return;

        firestoreManager.getJarById(jarId, new JarFirestoreManager.FirestoreCallback<JarFirestoreModel>() {
            @Override
            public void onSuccess(JarFirestoreModel jar) {
                if (jar != null) {
                    totalSaved = jar.getSavedAmount();
                    totalGoal = jar.getGoalAmount();
                    jarName = jar.getName();
                    jarType = jar.getType();

                    if (currentUser != null) {
                        Map<String, Double> contributions = jar.getParticipantContributions();
                        if (contributions != null) {
                            Double contribution = contributions.get(currentUser.getUid());
                            myContribution = contribution != null ? contribution : 0.0;
                        }
                    }

                    updateMainDisplay();
                    loadParticipants(jar);
                }
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(SharedJarsDetails.this,
                        "Error loading jar: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                Log.e("SharedJarsDetails", "Error loading jar", e);
            }
        });
    }

    private void loadTransactions() {
        if (jarId == null) return;

        firestoreManager.getTransactions(jarId, new JarFirestoreManager.FirestoreCallback<List<JarTransaction>>() {
            @Override
            public void onSuccess(List<JarTransaction> transactions) {
                transactionList.clear();
                transactionList.addAll(transactions);

                // Load user names for all transactions
                loadUserNamesForTransactions();
            }

            @Override
            public void onFailure(Exception e) {
                Log.e("SharedJarsDetails", "Error loading transactions", e);
            }
        });
    }

    private void loadUserNamesForTransactions() {
        List<String> userIds = new ArrayList<>();
        for (JarTransaction transaction : transactionList) {
            String userId = transaction.getUserId();
            if (!userIds.contains(userId) && !userNamesCache.containsKey(userId)) {
                userIds.add(userId);
            }
        }

        if (userIds.isEmpty()) {
            if (transactionAdapter != null) {
                transactionAdapter.notifyDataSetChanged();
            }
            return;
        }

        // Fetch user names from Firestore
        for (String userId : userIds) {
            db.collection("users").document(userId).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String userName = getUserDisplayName(documentSnapshot);
                            userNamesCache.put(userId, userName);
                        } else {
                            userNamesCache.put(userId, "User");
                        }

                        // Check if all names are loaded
                        boolean allLoaded = true;
                        for (JarTransaction t : transactionList) {
                            if (!userNamesCache.containsKey(t.getUserId())) {
                                allLoaded = false;
                                break;
                            }
                        }

                        if (allLoaded && transactionAdapter != null) {
                            transactionAdapter.notifyDataSetChanged();
                        }
                    })
                    .addOnFailureListener(e -> {
                        userNamesCache.put(userId, "User");
                        if (transactionAdapter != null) {
                            transactionAdapter.notifyDataSetChanged();
                        }
                    });
        }
    }

    private String getUserDisplayName(DocumentSnapshot userDoc) {
        String fullName = userDoc.getString("fullName");
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName;
        }

        String userName = userDoc.getString("userName");
        if (userName != null && !userName.trim().isEmpty()) {
            return userName;
        }

        String displayName = userDoc.getString("displayName");
        if (displayName != null && !displayName.trim().isEmpty()) {
            return displayName;
        }

        String name = userDoc.getString("name");
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }

        return "User";
    }

    private void loadParticipants(JarFirestoreModel jar) {
        if (jar == null || participantsContainer == null) return;

        participantsContainer.removeAllViews();

        Map<String, Double> contributions = jar.getParticipantContributions();
        List<String> participants = jar.getParticipants();

        if (contributions == null || contributions.isEmpty()) {
            if (currentUser != null) {
                addParticipantView(currentUser.getUid(), "You", myContribution, 0.0);
            }
            return;
        }

        int participantCount = participants != null ? participants.size() : 0;
        double suggestedAmount = participantCount > 0 ? totalGoal / participantCount : totalGoal;

        for (String userId : participants) {
            Double contributionValue = contributions.get(userId);
            double safeContributionValue = contributionValue != null ? contributionValue : 0.0;

            if (currentUser != null && userId.equals(currentUser.getUid())) {
                addParticipantView(userId, "You", safeContributionValue, suggestedAmount);
                myContribution = safeContributionValue;
            } else {
                // Load participant name from Firestore
                String finalUserId = userId;
                db.collection("users").document(userId).get()
                        .addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                String name = getUserDisplayName(documentSnapshot);
                                // Update the view with the actual name
                                updateParticipantName(finalUserId, name, safeContributionValue);
                            }
                        });
                addParticipantView(userId, "Loading...", safeContributionValue, 0.0);
            }
        }

        if (progressSuggestionText != null && participantCount > 0) {
            String formattedSuggested = formatCurrency(suggestedAmount);
            String message = "Suggested: " + formattedSuggested + " each";
            progressSuggestionText.setText(message);
        }
    }

    private void updateParticipantName(String userId, String name, double contribution) {
        // Find and update the participant view
        for (int i = 0; i < participantsContainer.getChildCount(); i++) {
            View participantView = participantsContainer.getChildAt(i);
            TextView participantName = participantView.findViewById(R.id.participant_name);
            Object tag = participantView.getTag();

            if (tag != null && tag.equals(userId)) {
                participantName.setText(name);
                break;
            }
        }
    }

    private void addParticipantView(String userId, String name, double contribution, double suggestedAmount) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View participantView = inflater.inflate(R.layout.item_participant, participantsContainer, false);

        // Set tag with userId for later updates
        participantView.setTag(userId);

        ImageView avatar = participantView.findViewById(R.id.participant_avatar);
        TextView participantName = participantView.findViewById(R.id.participant_name);
        TextView participantContribution = participantView.findViewById(R.id.participant_contribution);
        TextView participantSuggested = participantView.findViewById(R.id.participant_suggested);

        participantName.setText(name);
        participantContribution.setText(formatCurrency(contribution));

        if (currentUser != null && userId.equals(currentUser.getUid())) {
            avatar.setBackgroundTintList(ColorStateList.valueOf(color));
            if (suggestedAmount > 0) {
                participantSuggested.setVisibility(View.VISIBLE);
                String suggestedText = "Suggested: " + formatCurrency(suggestedAmount);
                participantSuggested.setText(suggestedText);
            }
        } else {
            avatar.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.gray)));
            participantSuggested.setVisibility(View.GONE);
        }

        participantsContainer.addView(participantView);
    }

    private void updateMainDisplay() {
        if (totalSavedView != null) totalSavedView.setText(formatCurrency(totalSaved));
        if (totalGoalView != null) totalGoalView.setText("Goal: " + formatCurrency(totalGoal));

        if (totalGoal > 0) {
            double progressRaw = (totalSaved / totalGoal) * 100;
            int progress = (int) Math.min(progressRaw, 100);

            if (progressBar != null) {
                progressBar.setMax(100);
                progressBar.setProgressCompat(progress, true);
                progressBar.setIndicatorColor(color);
            }
            if (progressPercentage != null) {
                progressPercentage.setText(progress + "% complete");
            }
        }

        if (jarTypeView != null && jarTypeCard != null) {
            int bgColor;
            int textColor;

            if ("Mandatory".equals(jarType)) {
                bgColor = R.color.red_light;
                textColor = R.color.red_dark;
            } else if ("Flexible".equals(jarType)) {
                bgColor = R.color.green_light;
                textColor = R.color.green_dark;
            } else {
                bgColor = R.color.blue_light;
                textColor = R.color.blue;
            }

            jarTypeCard.setCardBackgroundColor(ContextCompat.getColor(this, bgColor));
            jarTypeView.setTextColor(ContextCompat.getColor(this, textColor));
        }
    }

    private void showAddMoneyDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_money, null);

        EditText amountEditText = dialogView.findViewById(R.id.amount_edit_text);
        MaterialButton cancelButton = dialogView.findViewById(R.id.cancel_button);
        MaterialButton addButton = dialogView.findViewById(R.id.add_button);
        TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);

        if (dialogTitle != null) {
            dialogTitle.setText("Add to " + jarName);
        }

        amountEditText.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(current)) {
                    amountEditText.removeTextChangedListener(this);

                    String cleanString = s.toString().replaceAll("[^\\d.]", "");

                    if (cleanString.chars().filter(ch -> ch == '.').count() > 1) {
                        cleanString = cleanString.substring(0, cleanString.length() - 1);
                    }

                    if (!cleanString.isEmpty() && !cleanString.equals(".")) {
                        String symbol = SettingHelper.getCurrencySymbol(SettingHelper.getCurrency(SharedJarsDetails.this));

                        if (cleanString.contains(".")) {
                            current = symbol + cleanString;
                        } else {
                            double parsed = Double.parseDouble(cleanString);
                            current = symbol + String.format(Locale.US, "%,.0f", parsed);
                        }
                        amountEditText.setText(current);
                        amountEditText.setSelection(current.length());
                    } else {
                        current = "";
                        amountEditText.setText("");
                    }
                    amountEditText.addTextChangedListener(this);
                }
            }
        });

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        cancelButton.setOnClickListener(v -> dialog.dismiss());
        addButton.setOnClickListener(v -> {
            String amountStr = amountEditText.getText().toString().replaceAll("[^\\d.]", "");
            if (!amountStr.isEmpty()) {
                try {
                    double uiAmount = Double.parseDouble(amountStr);
                    if (uiAmount > 0) {
                        double amountInPHP = SettingHelper.convertToBasePHP(SharedJarsDetails.this, uiAmount);
                        addMoney(amountInPHP);
                        dialog.dismiss();
                    } else {
                        Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show();
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Invalid amount format", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show();
            }
        });

        dialog.show();
    }

    private void addMoney(double amountInPHP) {
        if (totalSaved + amountInPHP > totalGoal) {
            double remaining = totalGoal - totalSaved;
            Toast.makeText(this,
                    "Only " + formatCurrency(remaining) + " needed to reach goal",
                    Toast.LENGTH_LONG).show();
            return;
        }

        if (jarId == null) return;

        // Use JarFirestoreManager to add money
        firestoreManager.addMoney(jarId, amountInPHP, "Added money to jar",
                new JarFirestoreManager.FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        // Update local variables
                        totalSaved += amountInPHP;
                        myContribution += amountInPHP;

                        updateMainDisplay();

                        // Reload jar details to get updated data
                        loadJarDetails();
                        loadTransactions();

                        Toast.makeText(SharedJarsDetails.this,
                                "Added " + formatCurrency(amountInPHP),
                                Toast.LENGTH_SHORT).show();

                        if (totalSaved >= totalGoal) {
                            showSuccessDialog();
                        }
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(SharedJarsDetails.this,
                                "Error adding money: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        Log.e("SharedJarsDetails", "Error adding money", e);
                    }
                });
    }

    private void showInviteDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_invite, null);

        com.google.android.material.textfield.TextInputEditText emailEditText =
                dialogView.findViewById(R.id.email_edit_text);
        ImageButton btnClose = dialogView.findViewById(R.id.btn_close);
        MaterialButton sendButton = dialogView.findViewById(R.id.send_button);
        MaterialButton shareLinkButton = dialogView.findViewById(R.id.share_link_button);
        TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);

        if (dialogTitle != null) {
            dialogTitle.setText("Invite to " + jarName);
        }

        AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        btnClose.setOnClickListener(v -> dialog.dismiss());

        sendButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            if (!email.isEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                firestoreManager.addParticipant(jarId, email, new JarFirestoreManager.FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        Toast.makeText(SharedJarsDetails.this,
                                "Invitation sent to " + email,
                                Toast.LENGTH_SHORT).show();
                        loadJarDetails();
                        dialog.dismiss();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(SharedJarsDetails.this,
                                "Error: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                emailEditText.setError("Valid email required");
            }
        });

        shareLinkButton.setOnClickListener(v -> {
            shareJarInvite();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void shareJarInvite() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Join my jar: " + jarName);
        shareIntent.putExtra(Intent.EXTRA_TEXT,
                "Join my jar '" + jarName + "' with goal of " + formatCurrency(totalGoal) +
                        ". Current savings: " + formatCurrency(totalSaved));

        startActivity(Intent.createChooser(shareIntent, "Invite to Jar"));
    }

    private void showSuccessDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Congratulations!")
                .setMessage("You've reached your goal for " + jarName + "!")
                .setPositiveButton("Awesome", (dialog, which) -> dialog.dismiss())
                .setCancelable(false)
                .show();
    }

    private void setupClickListeners() {
        ImageButton backButton = findViewById(R.id.back_button);
        if (backButton != null) backButton.setOnClickListener(v -> sendResultBack());

        if (addMoneyButton != null) {
            addMoneyButton.setOnClickListener(v -> showAddMoneyDialog());
        }

        if (viewActivityButton != null) {
            viewActivityButton.setOnClickListener(v -> openActivityLog());
        }

        if (inviteButton != null) {
            inviteButton.setOnClickListener(v -> showInviteDialog());
        }
    }

    private void openActivityLog() {
        Intent intent = new Intent(this, ActivityLogActivity.class);
        intent.putExtra("JAR_ID", jarId);
        intent.putExtra("JAR_NAME", jarName);
        startActivity(intent);
    }

    private void sendResultBack() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("UPDATED_ID", jarId);
        resultIntent.putExtra("UPDATED_SAVED", totalSaved);
        setResult(Activity.RESULT_OK, resultIntent);
        finish();
    }

    private String formatCurrency(double amountInPHP) {
        return SettingHelper.formatAmount(this, amountInPHP);
    }

    private int getIconResourceId(String iconName) {
        if (iconName == null) return R.drawable.hat;
        switch (iconName) {
            case "house": return R.drawable.house;
            case "plane":
            case "airplane": return R.drawable.airplane;
            case "laptop": return R.drawable.laptop;
            case "car": return R.drawable.car;
            case "game":
            case "controller": return R.drawable.controller;
            case "shirt":
            case "tshirt": return R.drawable.tshirt;
            case "book": return R.drawable.book;
            case "burger": return R.drawable.burger;
            case "gift": return R.drawable.ic_gift;
            default: return R.drawable.hat;
        }
    }

    private class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder> {
        private final List<JarTransaction> transactions;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());

        TransactionAdapter(List<JarTransaction> transactions) {
            this.transactions = transactions;
        }

        @NonNull
        @Override
        public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_transaction, parent, false);
            return new TransactionViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
            JarTransaction transaction = transactions.get(position);

            // Get user name from cache
            String userName = userNamesCache.get(transaction.getUserId());
            if (userName == null) {
                userName = transaction.getUserName();
            }

            if (userName == null || userName.isEmpty()) {
                userName = "User";
            }

            // Show if it's the current user
            if (currentUser != null && transaction.getUserId().equals(currentUser.getUid())) {
                userName = userName + " (You)";
                holder.textTitle.setTextColor(color);
            } else {
                holder.textTitle.setTextColor(
                        ContextCompat.getColor(SharedJarsDetails.this, R.color.text_primary));
            }

            holder.textTitle.setText(userName);
            holder.textAmount.setText("+ " + formatCurrency(transaction.getAmount()));

            String note = transaction.getNote();
            if (note != null && !note.isEmpty()) {
                holder.textNote.setText(note);
                holder.textNote.setVisibility(View.VISIBLE);
            } else {
                holder.textNote.setVisibility(View.GONE);
            }

            if (transaction.getTimestamp() != null) {
                Date date = transaction.getTimestamp().toDate();
                holder.textDate.setText(dateFormat.format(date));
            } else {
                holder.textDate.setText("Just now");
            }

            // Set appropriate icon based on transaction type
            if ("CREATE".equals(transaction.getType())) {
                holder.iconImage.setImageResource(R.drawable.ic_gift);
                holder.iconBackgroundCard.setCardBackgroundColor(
                        ContextCompat.getColor(SharedJarsDetails.this, R.color.purple_light));
            } else if ("ADD_PARTICIPANT".equals(transaction.getType())) {
                holder.iconImage.setImageResource(R.drawable.ic_add_person);
                holder.iconBackgroundCard.setCardBackgroundColor(
                        ContextCompat.getColor(SharedJarsDetails.this, R.color.orange_light));
            } else {
                holder.iconImage.setImageResource(R.drawable.ic_add);
                holder.iconBackgroundCard.setCardBackgroundColor(
                        ContextCompat.getColor(SharedJarsDetails.this, R.color.blue_light));
            }
        }

        @Override
        public int getItemCount() {
            return transactions.size();
        }

        class TransactionViewHolder extends RecyclerView.ViewHolder {
            final CardView iconBackgroundCard;
            final ImageView iconImage;
            final TextView textTitle, textDate, textAmount, textNote;

            TransactionViewHolder(@NonNull View itemView) {
                super(itemView);
                iconBackgroundCard = itemView.findViewById(R.id.icon_background_card);
                iconImage = itemView.findViewById(R.id.icon_image);
                textTitle = itemView.findViewById(R.id.text_title);
                textDate = itemView.findViewById(R.id.text_date);
                textAmount = itemView.findViewById(R.id.text_amount);
                textNote = itemView.findViewById(R.id.text_note);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadJarDetails();
        loadTransactions();
    }
}