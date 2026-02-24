package com.example.fintrac;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class sign_in extends AppCompatActivity {

    EditText passwordInput, confirmPasswordInput, emailInput, fullNameInput;
    CheckBox termsCheckbox;
    ImageView eyePassword, eyeConfirm;

    private FirebaseAuth mAuth;
    private FirestoreHelper firestoreHelper;

    boolean isPasswordVisible = false;
    boolean isConfirmPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SettingHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        firestoreHelper = FirestoreHelper.getInstance();

        emailInput = findViewById(R.id.email_input);
        fullNameInput = findViewById(R.id.full_name_input);
        passwordInput = findViewById(R.id.password_input);
        confirmPasswordInput = findViewById(R.id.confirm_password_input);
        termsCheckbox = findViewById(R.id.terms_checkbox);

        // Back button
        TextView backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        // Make "Sign in" text blue and clickable
        setupSignInLink();

        // Create account button
        Button createAccountButton = findViewById(R.id.create_account_button);
        if (createAccountButton != null) {
            createAccountButton.setOnClickListener(v -> registerUser());
        }

        // Setup Terms checkbox with clickable link
        setupTermsCheckbox();

        // Setup password toggles
        setupPasswordToggles();
    }

    private void setupSignInLink() {
        TextView tvHaveAccount = findViewById(R.id.tvHaveAccount);
        if (tvHaveAccount != null) {
            String text = "I have an account? Sign in";
            SpannableString spannable = new SpannableString(text);

            int blueColor = ContextCompat.getColor(this, R.color.blue);

            int signInStart = text.indexOf("Sign in");
            int signInEnd = signInStart + "Sign in".length();

            ClickableSpan signInSpan = new ClickableSpan() {
                @Override
                public void onClick(View widget) {
                    Intent intent = new Intent(sign_in.this, LogIn.class);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setColor(blueColor);
                    ds.setUnderlineText(false);
                }
            };

            spannable.setSpan(signInSpan, signInStart, signInEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            tvHaveAccount.setText(spannable);
            tvHaveAccount.setMovementMethod(LinkMovementMethod.getInstance());
            tvHaveAccount.setHighlightColor(android.graphics.Color.TRANSPARENT);
        }
    }

    /**
     * Setup checkbox with clickable "Terms & Conditions and Privacy Policy" link
     * Clicking the link opens the dialog
     * User must check the checkbox manually after reading
     */
    private void setupTermsCheckbox() {
        if (termsCheckbox == null) return;

        String fullText = "I agree to the Terms & Conditions and Privacy Policy";
        SpannableString spannable = new SpannableString(fullText);

        int blueColor = ContextCompat.getColor(this, R.color.blue);

        // Find the clickable text range
        int termsStart = fullText.indexOf("Terms & Conditions");
        int privacyEnd = fullText.indexOf("Privacy Policy") + "Privacy Policy".length();

        ClickableSpan termsSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                // Open the Terms & Privacy dialog
                TermsAndConditions.showTermsDialog(sign_in.this, termsCheckbox);
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(blueColor);
                ds.setUnderlineText(false);
            }
        };

        spannable.setSpan(termsSpan, termsStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        termsCheckbox.setText(spannable);
        termsCheckbox.setMovementMethod(LinkMovementMethod.getInstance());
        termsCheckbox.setHighlightColor(android.graphics.Color.TRANSPARENT);
    }

    private void setupPasswordToggles() {
        eyePassword = findViewById(R.id.imageView6);
        eyeConfirm = findViewById(R.id.imageView4);

        if (eyePassword != null && passwordInput != null) {
            eyePassword.setOnClickListener(v -> {
                if (isPasswordVisible) {
                    passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    eyePassword.setImageResource(R.drawable.baseline_visibility_24);
                } else {
                    passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    eyePassword.setImageResource(R.drawable.baseline_visibility_off_24);
                }
                passwordInput.setSelection(passwordInput.getText().length());
                isPasswordVisible = !isPasswordVisible;
            });
        }

        if (eyeConfirm != null && confirmPasswordInput != null) {
            eyeConfirm.setOnClickListener(v -> {
                if (isConfirmPasswordVisible) {
                    confirmPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    eyeConfirm.setImageResource(R.drawable.baseline_visibility_24);
                } else {
                    confirmPasswordInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    eyeConfirm.setImageResource(R.drawable.baseline_visibility_off_24);
                }
                confirmPasswordInput.setSelection(confirmPasswordInput.getText().length());
                isConfirmPasswordVisible = !isConfirmPasswordVisible;
            });
        }
    }

    private void registerUser() {
        String email = emailInput.getText().toString().trim();
        String fullName = fullNameInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();
        boolean acceptedTerms = termsCheckbox.isChecked();

        if (!validateInputs(email, fullName, password, confirmPassword, acceptedTerms)) {
            return;
        }

        Button createAccountButton = findViewById(R.id.create_account_button);
        if (createAccountButton != null) {
            createAccountButton.setEnabled(false);
            createAccountButton.setText(R.string.creating_account);
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            updateUserProfile(user, fullName);
                        }
                    } else {
                        if (createAccountButton != null) {
                            createAccountButton.setEnabled(true);
                            createAccountButton.setText(R.string.create_account);
                        }
                        String errorMessage = getFriendlyErrorMessage(task.getException());
                        Toast.makeText(sign_in.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validateInputs(String email, String fullName, String password,
                                   String confirmPassword, boolean acceptedTerms) {
        if (email.isEmpty()) {
            emailInput.setError(getString(R.string.error_email_required));
            emailInput.requestFocus();
            return false;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError(getString(R.string.error_valid_email));
            emailInput.requestFocus();
            return false;
        }
        if (fullName.isEmpty()) {
            fullNameInput.setError(getString(R.string.error_name_required));
            fullNameInput.requestFocus();
            return false;
        }
        if (password.isEmpty()) {
            passwordInput.setError(getString(R.string.error_password_required));
            passwordInput.requestFocus();
            return false;
        }
        if (password.length() < 6) {
            passwordInput.setError(getString(R.string.error_password_length));
            passwordInput.requestFocus();
            return false;
        }
        if (confirmPassword.isEmpty()) {
            confirmPasswordInput.setError(getString(R.string.error_confirm_password));
            confirmPasswordInput.requestFocus();
            return false;
        }
        if (!password.equals(confirmPassword)) {
            confirmPasswordInput.setError(getString(R.string.error_password_mismatch));
            confirmPasswordInput.requestFocus();
            return false;
        }
        if (!acceptedTerms) {
            Toast.makeText(this, "Please accept the Terms & Conditions", Toast.LENGTH_SHORT).show();
            termsCheckbox.requestFocus();
            return false;
        }
        return true;
    }

    private void updateUserProfile(FirebaseUser user, String fullName) {
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build();

        user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                saveUserToFirestore(user, fullName);
            } else {
                Toast.makeText(sign_in.this,
                        R.string.account_created_profile_failed,
                        Toast.LENGTH_SHORT).show();
                saveUserToFirestore(user, fullName);
            }
        });
    }

    private void saveUserToFirestore(FirebaseUser user, String fullName) {
        firestoreHelper.saveUserData(user.getUid(), fullName, user.getEmail(),
                new FirestoreHelper.OnUserSavedListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(sign_in.this,
                                R.string.registration_successful,
                                Toast.LENGTH_SHORT).show();
                        sendEmailVerification(user);
                        navigateToDashboard();
                    }

                    @Override
                    public void onFailure(String error) {
                        Toast.makeText(sign_in.this,
                                getString(R.string.account_created_data_failed) + error,
                                Toast.LENGTH_SHORT).show();
                        navigateToDashboard();
                    }
                });
    }

    private void sendEmailVerification(FirebaseUser user) {
        user.sendEmailVerification().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(sign_in.this,
                        getString(R.string.verification_email_sent) + " " + user.getEmail(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(sign_in.this, dashboard.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String getFriendlyErrorMessage(Exception exception) {
        if (exception == null) return getString(R.string.error_registration_failed);
        String errorMessage = exception.getMessage();
        if (errorMessage != null) {
            if (errorMessage.contains("email address is already in use")) {
                return getString(R.string.error_email_in_use);
            } else if (errorMessage.contains("password is invalid") ||
                    errorMessage.contains("weak password")) {
                return getString(R.string.error_weak_password);
            } else if (errorMessage.contains("network error") ||
                    errorMessage.contains("no internet")) {
                return getString(R.string.error_network);
            } else if (errorMessage.contains("invalid email")) {
                return getString(R.string.error_invalid_email);
            }
        }
        return getString(R.string.error_registration_failed) + " " +
                (errorMessage != null ? errorMessage : getString(R.string.error_unknown));
    }

    @Override
    protected void onResume() {
        super.onResume();
        SettingHelper.applyTheme(this);
        Button createAccountButton = findViewById(R.id.create_account_button);
        if (createAccountButton != null) {
            createAccountButton.setEnabled(true);
            createAccountButton.setText(R.string.create_account);
        }
    }
}