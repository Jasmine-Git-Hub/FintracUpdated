package com.example.fintrac;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log; // Import para sa logs
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

// Import para sa Firebase
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply theme BEFORE super.onCreate and setContentView
        SettingHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- FIREBASE TEST CODE (JAVA VERSION) ---
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Gumawa ng data (Map ang gamit sa Java instead na hashMapOf)
        Map<String, Object> testData = new HashMap<>();
        testData.put("message", "Hello Firebase! (Java)");

        db.collection("test_connection")
                .add(testData)
                .addOnSuccessListener(documentReference -> {
                    // Ito ang lalabas sa Logcat kapag success
                    Log.d("FirebaseTest", "SUCCESS: Connected ka na sa Database!");
                })
                .addOnFailureListener(e -> {
                    // Ito ang lalabas kapag fail
                    Log.e("FirebaseTest", "ERROR: " + e.getMessage());
                });
        // ----------------------------------------

        // Initialize buttons
        Button getStartedButton = findViewById(R.id.get_started_button);
        Button alreadyHaveAccountButton = findViewById(R.id.already_have_account_button);

        // Set click listeners
        if (getStartedButton != null) {
            getStartedButton.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, sign_in.class);
                startActivity(intent);
            });
        }

        if (alreadyHaveAccountButton != null) {
            alreadyHaveAccountButton.setOnClickListener(v -> {
                Intent intent = new Intent(MainActivity.this, LogIn.class);
                startActivity(intent);
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Re-apply theme when returning to this activity
        SettingHelper.applyTheme(this);
    }

    /**
     * Method to select a bottom navigation item programmatically
     * This is called from fragments to navigate between tabs
     * @param menuItemId The ID of the menu item to select
     */
    public void selectBottomNavItem(int menuItemId) {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(menuItemId);
        } else {
            Log.e("MainActivity", "BottomNavigationView not found. Make sure your activity_main.xml contains a BottomNavigationView with id 'bottom_navigation'");
        }
    }
}