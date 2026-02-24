package com.example.fintrac;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LogIn extends AppCompatActivity {

    // Firebase Authentication
    private FirebaseAuth mAuth;

    // UI Components
    private EditText emailInput, passwordInput;
    private TextView forgotPasswordText, signUpText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SettingHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_log_in);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Initialize UI Components
        initializeViews();

        // Setup click listeners
        setupClickListeners();
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToDashboard();
        }
    }

    private void initializeViews() {
        emailInput = findViewById(R.id.email_input);
        passwordInput = findViewById(R.id.password_input);
        forgotPasswordText = findViewById(R.id.forgot_password_text);
        signUpText = findViewById(R.id.sign_up_text);
        // Wala na password_toggle — TextInputLayout na ang bahala doon
    }

    private void setupClickListeners() {
        // Back button
        TextView backButton = findViewById(R.id.back_button);
        if (backButton != null) {
            backButton.setOnClickListener(v -> finish());
        }

        // Forgot password
        if (forgotPasswordText != null) {
            forgotPasswordText.setOnClickListener(v -> showForgotPasswordDialog());
        }

        // Sign in button
        Button signInButton = findViewById(R.id.sign_in_button);
        if (signInButton != null) {
            signInButton.setOnClickListener(v -> loginUser());
        }

        // Sign up text
        if (signUpText != null) {
            signUpText.setOnClickListener(v -> {
                Intent intent = new Intent(LogIn.this, sign_in.class);
                startActivity(intent);
                finish();
            });
        }
    }

    private void loginUser() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (!validateInputs(email, password)) {
            return;
        }

        Button signInButton = findViewById(R.id.sign_in_button);
        if (signInButton != null) {
            signInButton.setEnabled(false);
            signInButton.setText("Signing in...");
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (signInButton != null) {
                        signInButton.setEnabled(true);
                        signInButton.setText("Sign In");
                    }

                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        handleSuccessfulLogin(user);
                    } else {
                        String errorMessage = getFriendlyErrorMessage(task.getException());
                        Toast.makeText(LogIn.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private boolean validateInputs(String email, String password) {
        if (email.isEmpty()) {
            emailInput.setError("Email is required");
            emailInput.requestFocus();
            return false;
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.setError("Enter a valid email");
            emailInput.requestFocus();
            return false;
        }

        if (password.isEmpty()) {
            passwordInput.setError("Password is required");
            passwordInput.requestFocus();
            return false;
        }

        return true;
    }

    private void handleSuccessfulLogin(FirebaseUser user) {
        if (user != null && !user.isEmailVerified()) {
            Toast.makeText(LogIn.this,
                    "Please verify your email address. Check your inbox for verification link.",
                    Toast.LENGTH_LONG).show();
        }

        Toast.makeText(LogIn.this, "Login successful! Welcome back.", Toast.LENGTH_SHORT).show();
        navigateToDashboard();
    }

    private String getFriendlyErrorMessage(Exception exception) {
        if (exception == null) return "Login failed";

        String errorMessage = exception.getMessage();

        if (errorMessage != null) {
            if (errorMessage.contains("invalid login credentials") ||
                    errorMessage.contains("no user record") ||
                    errorMessage.contains("password is invalid") ||
                    errorMessage.contains("ERROR_WRONG_PASSWORD") ||
                    errorMessage.contains("ERROR_USER_NOT_FOUND")) {
                return "Invalid email or password";
            } else if (errorMessage.contains("network error") ||
                    errorMessage.contains("no internet") ||
                    errorMessage.contains("Failed to connect")) {
                return "No internet connection. Please check your network.";
            } else if (errorMessage.contains("too many requests")) {
                return "Too many failed attempts. Please try again later.";
            } else if (errorMessage.contains("user disabled")) {
                return "This account has been disabled. Please contact support.";
            } else if (errorMessage.contains("user-not-found")) {
                return "No account found with this email. Please sign up first.";
            } else if (errorMessage.contains("invalid-email")) {
                return "Invalid email format.";
            }
        }

        return "Login failed: " + (errorMessage != null ? errorMessage : "Unknown error");
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(LogIn.this, dashboard.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void showForgotPasswordDialog() {
        String email = emailInput.getText().toString().trim();

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            emailInput.requestFocus();
            return;
        }

        Button signInButton = findViewById(R.id.sign_in_button);
        if (signInButton != null) {
            signInButton.setEnabled(false);
            signInButton.setText("Sending...");
        }

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (signInButton != null) {
                        signInButton.setEnabled(true);
                        signInButton.setText("Sign In");
                    }

                    if (task.isSuccessful()) {
                        Toast.makeText(LogIn.this,
                                "Password reset link has been sent to:\n" + email,
                                Toast.LENGTH_LONG).show();
                    } else {
                        String errorMessage = "Failed to send reset email";
                        if (task.getException() != null) {
                            String exceptionMsg = task.getException().getMessage();
                            if (exceptionMsg != null) {
                                if (exceptionMsg.contains("user-not-found")) {
                                    errorMessage = "No account found with this email";
                                } else if (exceptionMsg.contains("network error")) {
                                    errorMessage = "Network error. Please check your connection.";
                                } else {
                                    errorMessage = exceptionMsg;
                                }
                            }
                        }
                        Toast.makeText(LogIn.this, errorMessage, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        SettingHelper.applyTheme(this);

        Button signInButton = findViewById(R.id.sign_in_button);
        if (signInButton != null) {
            signInButton.setEnabled(true);
            signInButton.setText("Sign In");
        }
    }
}