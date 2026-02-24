package com.example.fintrac;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

public class TermsAndConditions {

    /**
     * Shows Terms & Privacy Policy dialog with card-style boxes for each section
     * Matches the "Start Challenge" design with individual cards
     */
    public static void showTermsDialog(Context context, CheckBox termsCheckbox) {
        // Check if dark mode is enabled
        boolean isDarkMode = SettingHelper.isDarkMode(context);

        // Color scheme matching your app
        int bgColor = isDarkMode ? Color.parseColor("#1A1A1A") : Color.parseColor("#FFFFFF");
        int textColor = isDarkMode ? Color.parseColor("#FFFFFF") : Color.parseColor("#1A1A1A");
        int cardBgColor = isDarkMode ? Color.parseColor("#252525") : Color.parseColor("#F8F8F8");
        int dividerColor = isDarkMode ? Color.parseColor("#2A2A2A") : Color.parseColor("#F0F0F0");
        int accentColor = Color.parseColor("#0080FF"); // Your blue
        int secondaryTextColor = isDarkMode ? Color.parseColor("#CCCCCC") : Color.parseColor("#666666");

        // --- Root container ---
        LinearLayout rootLayout = new LinearLayout(context);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(bgColor);

        GradientDrawable cardBackground = new GradientDrawable();
        cardBackground.setColor(bgColor);
        cardBackground.setCornerRadius(dpToPx(context, 16));
        rootLayout.setBackground(cardBackground);

        int padding = dpToPx(context, 16);
        rootLayout.setPadding(padding, dpToPx(context, 16), padding, dpToPx(context, 16));

        // --- Header Row: Title + X button ---
        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView titleText = new TextView(context);
        titleText.setText("Terms & Privacy");
        titleText.setTextSize(18f);
        titleText.setTextColor(textColor);
        titleText.setTypeface(null, android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleText.setLayoutParams(titleParams);

        TextView closeBtn = new TextView(context);
        closeBtn.setText("✕");
        closeBtn.setTextSize(26f);
        closeBtn.setTextColor(accentColor);
        closeBtn.setPadding(dpToPx(context, 8), 0, 0, 0);

        headerRow.addView(titleText);
        headerRow.addView(closeBtn);

        // --- Divider ---
        View divider = new View(context);
        divider.setBackgroundColor(dividerColor);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(context, 1));
        dividerParams.setMargins(0, dpToPx(context, 12), 0, dpToPx(context, 12));
        divider.setLayoutParams(dividerParams);

        // --- ScrollView with card items ---
        ScrollView scrollView = new ScrollView(context);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dpToPx(context, 480));
        scrollView.setLayoutParams(scrollParams);

        // --- Content container (vertical layout for cards) ---
        LinearLayout contentContainer = new LinearLayout(context);
        contentContainer.setOrientation(LinearLayout.VERTICAL);
        contentContainer.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ===== WELCOME CARD =====
        contentContainer.addView(createTitleCard(context, "What are Terms and Conditions?",
                "At its core, a T&C document sets the expectations for how a service should be used and what happens if someone breaks those rules. It usually covers user guidelines, intellectual property, liability limits, payment rules, and account termination policies.",
                cardBgColor, textColor, secondaryTextColor));

        // ===== SECTION 1 =====
        contentContainer.addView(createSectionCard(context, "1. User Guidelines",
                "You agree to use Fintrac responsibly and legally:\n\n" +
                        "✓ No spam, harassment, or abusive behavior\n" +
                        "✓ No fraudulent or illegal activities\n" +
                        "✓ No hacking or unauthorized access attempts\n" +
                        "✓ No posting hate speech or offensive content\n" +
                        "✓ Respect other users' privacy and data",
                cardBgColor, accentColor, textColor, secondaryTextColor));

        // ===== SECTION 2 =====
        contentContainer.addView(createSectionCard(context, "2. Intellectual Property Rights",
                "Our intellectual property:\n\n" +
                        "✓ The Fintrac logo belongs to Fintrac\n" +
                        "✓ All app content and design are protected\n" +
                        "✓ You cannot copy or reproduce our materials\n" +
                        "✓ Unauthorized use may result in legal action",
                cardBgColor, accentColor, textColor, secondaryTextColor));

        // ===== SECTION 3 =====
        contentContainer.addView(createSectionCard(context, "3. Liability Limits",
                "Important legal protections:\n\n" +
                        "✓ We protect our business from lawsuits\n" +
                        "✓ We're not liable for technical glitches\n" +
                        "✓ We're not responsible if you lose data\n" +
                        "✓ We're not liable for indirect damages\n" +
                        "✓ Use the app at your own risk",
                cardBgColor, accentColor, textColor, secondaryTextColor));

        // ===== SECTION 4 =====
        contentContainer.addView(createSectionCard(context, "4. Account Security & Responsibility",
                "Your account, your responsibility:\n\n" +
                        "✓ You must be at least 18 years old\n" +
                        "✓ Keep your password confidential and secure\n" +
                        "✓ Don't share your account with others\n" +
                        "✓ You're responsible for all account activity\n" +
                        "✓ Report suspicious activity immediately",
                cardBgColor, accentColor, textColor, secondaryTextColor));

        // ===== SECTION 5 =====
        contentContainer.addView(createSectionCard(context, "5. Payment & Billing (Future Feature)",
                "For future transactions:\n\n" +
                        "✓ All prices are in Philippine Pesos (₱)\n" +
                        "✓ Refunds may be available within 30 days\n" +
                        "✓ Cancellations require written notice\n" +
                        "✓ Billing cycles are monthly or as agreed\n" +
                        "✓ Payment information is kept secure",
                cardBgColor, accentColor, textColor, secondaryTextColor));

        // ===== SECTION 6 =====
        contentContainer.addView(createSectionCard(context, "6. Data Privacy & Protection",
                "Your data is important to us:\n\n" +
                        "✓ We comply with Philippine Data Privacy Act (RA 10173)\n" +
                        "✓ We use industry-standard encryption\n" +
                        "✓ We do NOT sell your personal data\n" +
                        "✓ Data is only shared with your permission\n" +
                        "✓ You can request data deletion anytime",
                cardBgColor, accentColor, textColor, secondaryTextColor));

        // ===== SECTION 7 =====
        contentContainer.addView(createSectionCard(context, "7. Your Privacy Rights",
                "You have the right to:\n\n" +
                        "✓ Access your personal data anytime\n" +
                        "✓ Correct inaccurate information\n" +
                        "✓ Request complete account deletion\n" +
                        "✓ Object to data processing\n" +
                        "✓ Know how your data is being used",
                cardBgColor, accentColor, textColor, secondaryTextColor));

        // ===== SECTION 8 =====
        contentContainer.addView(createSectionCard(context, "8. Shared Jars & Collaborative Features",
                "When using Shared Jars:\n\n" +
                        "✓ Other users can see goal amounts and progress\n" +
                        "✓ Contribution history is visible to participants\n" +
                        "✓ You control who has access\n" +
                        "✓ Fintrac is not responsible for user disputes\n" +
                        "✓ Be cautious when sharing financial data",
                cardBgColor, accentColor, textColor, secondaryTextColor));

        // ===== SECTION 9 =====
        contentContainer.addView(createSectionCard(context, "9. Account Suspension & Termination",
                "Your account may be suspended if you:\n\n" +
                        "✓ Violate these Terms and Conditions\n" +
                        "✓ Engage in fraudulent or illegal activity\n" +
                        "✓ Attempt to breach security systems\n" +
                        "✓ Harass other users\n" +
                        "✓ Violate applicable laws",
                cardBgColor, accentColor, textColor, secondaryTextColor));

        // ===== SECTION 10 =====
        contentContainer.addView(createSectionCard(context, "10. Modifications & Updates",
                "About future changes:\n\n" +
                        "✓ We may update these terms at any time\n" +
                        "✓ You'll be notified of major changes\n" +
                        "✓ Continuing to use the app means acceptance\n" +
                        "✓ We'll always act in your best interest\n" +
                        "✓ Your rights are protected by law",
                cardBgColor, accentColor, textColor, secondaryTextColor));

        // ===== SECTION 11 =====
        contentContainer.addView(createSectionCard(context, "11. Disclaimer & No Financial Advice",
                "Important disclaimers:\n\n" +
                        "✓ Fintrac is NOT a bank\n" +
                        "✓ We do NOT provide financial advice\n" +
                        "✓ We do NOT provide investment guidance\n" +
                        "✓ We do NOT hold or manage your money\n" +
                        "✓ Challenges are for motivation only",
                cardBgColor, accentColor, textColor, secondaryTextColor));

        // ===== SECTION 12 =====
        contentContainer.addView(createSectionCard(context, "12. Governing Law & Jurisdiction",
                "Legal framework:\n\n" +
                        "✓ These terms follow Philippine law\n" +
                        "✓ Disputes are resolved in Philippine courts\n" +
                        "✓ This agreement is binding and enforceable\n" +
                        "✓ Both parties agree to these terms\n" +
                        "✓ Your usage implies acceptance",
                cardBgColor, accentColor, textColor, secondaryTextColor));

        // ===== SECTION 13 =====
        contentContainer.addView(createSectionCard(context, "13. Contact & Support",
                "Questions or concerns?\n\n" +
                        "Email: support@fintrac.ph\n" +
                        "Response time: Within 48 hours\n\n" +
                        "Data Protection Officer:\n" +
                        "Email: privacy@fintrac.ph\n\n" +
                        "We're here to help and protect your rights!",
                cardBgColor, accentColor, textColor, secondaryTextColor));

        scrollView.addView(contentContainer);

        // --- Assemble layout ---
        rootLayout.addView(headerRow);
        rootLayout.addView(divider);
        rootLayout.addView(scrollView);

        // --- Build & show dialog ---
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(rootLayout)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(dialog.getWindow().getAttributes());
            layoutParams.width = (int) (context.getResources().getDisplayMetrics().widthPixels * 0.95);
            dialog.getWindow().setAttributes(layoutParams);
        }

        dialog.show();
        animateDialogEntrance(rootLayout);

        closeBtn.setOnClickListener(v -> dialog.dismiss());
    }

    /**
     * Create a title/welcome card
     */
    private static View createTitleCard(Context context, String title, String description,
                                        int cardBgColor, int textColor, int secondaryTextColor) {
        LinearLayout cardLayout = new LinearLayout(context);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setBackgroundColor(cardBgColor);

        GradientDrawable cardDrawable = new GradientDrawable();
        cardDrawable.setColor(cardBgColor);
        cardDrawable.setCornerRadius(dpToPx(context, 12));
        cardLayout.setBackground(cardDrawable);

        int padding = dpToPx(context, 14);
        cardLayout.setPadding(padding, padding, padding, padding);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dpToPx(context, 12));
        cardLayout.setLayoutParams(cardParams);

        // Title
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(15f);
        titleView.setTextColor(textColor);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Description
        TextView descView = new TextView(context);
        descView.setText(description);
        descView.setTextSize(12.5f);
        descView.setTextColor(secondaryTextColor);
        descView.setLineSpacing(4f, 1.2f);
        LinearLayout.LayoutParams descParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descParams.setMargins(0, dpToPx(context, 8), 0, 0);
        descView.setLayoutParams(descParams);

        cardLayout.addView(titleView);
        cardLayout.addView(descView);

        return cardLayout;
    }

    /**
     * Create a section card with number, title, and content
     */
    private static View createSectionCard(Context context, String title, String content,
                                          int cardBgColor, int accentColor, int textColor, int secondaryTextColor) {
        LinearLayout cardLayout = new LinearLayout(context);
        cardLayout.setOrientation(LinearLayout.VERTICAL);
        cardLayout.setBackgroundColor(cardBgColor);

        GradientDrawable cardDrawable = new GradientDrawable();
        cardDrawable.setColor(cardBgColor);
        cardDrawable.setCornerRadius(dpToPx(context, 12));
        cardLayout.setBackground(cardDrawable);

        int padding = dpToPx(context, 14);
        cardLayout.setPadding(padding, padding, padding, padding);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cardParams.setMargins(0, 0, 0, dpToPx(context, 12));
        cardLayout.setLayoutParams(cardParams);

        // Section title (colored)
        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(14f);
        titleView.setTextColor(accentColor);
        titleView.setTypeface(null, android.graphics.Typeface.BOLD);
        titleView.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Content
        TextView contentView = new TextView(context);
        contentView.setText(content);
        contentView.setTextSize(12.5f);
        contentView.setTextColor(secondaryTextColor);
        contentView.setLineSpacing(5f, 1.2f);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        contentParams.setMargins(0, dpToPx(context, 8), 0, 0);
        contentView.setLayoutParams(contentParams);

        cardLayout.addView(titleView);
        cardLayout.addView(contentView);

        return cardLayout;
    }

    /**
     * Animate dialog entrance
     */
    private static void animateDialogEntrance(View view) {
        view.setAlpha(0f);
        view.setScaleX(0.85f);
        view.setScaleY(0.85f);

        view.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(300)
                .start();
    }

    // Helper: dp to px
    private static int dpToPx(Context context, int dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
}