package com.vaibhav.hashly;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

public class NicheSelectorActivity extends AppCompatActivity {

    private Spinner nicheSpinner;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_niche_selector);

        db = FirebaseFirestore.getInstance();
        nicheSpinner = findViewById(R.id.nicheSpinner);

        // 🔥 FIXED: Add "Select Niche" as FIRST OPTION + Alphabetical order
        String[] niches = {
                "Select Niche",  // ← Shows first by default!
                "beauty", "business", "cars", "cartoon", "comedy", "cooking",
                "dance", "fitness", "food", "gaming", "luxury", "memes",
                "music", "parenting", "pets", "photography", "quotes",
                "sports", "technology", "travel", "wellness"
        };

        // Setup dropdown
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, niches);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        nicheSpinner.setAdapter(adapter);

        // 🔥 CRITICAL: Set "Select Niche" as default selection (position 0)
        nicheSpinner.setSelection(0);

        // 🔥 IMPROVED: Clean listener - no firstSelection flag needed
        nicheSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedNiche = parent.getItemAtPosition(position).toString();

                // 🔥 PERFECT: Skip "Select Niche" completely
                if (selectedNiche.equals("Select Niche") || position == 0) {
                    return; // Do nothing - stays on screen
                }

                // 🔥 Only real niches trigger loading
                fetchHashtagsForNiche(selectedNiche);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void fetchHashtagsForNiche(String niche) {
        // 🔥 EXTRA SAFETY CHECK
        if (niche.equals("Select Niche")) {
            return;
        }

        Toast.makeText(this, "🔥 Loading " + niche + " hashtags...", Toast.LENGTH_SHORT).show();

        // 🔥 Your Firebase structure: trends/hashly/niches/{niche}
        db.collection("trends")
                .document("hashly")
                .collection("niches")
                .document(niche)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Toast.makeText(this, "✅ " + niche + " hashtags loaded!", Toast.LENGTH_SHORT).show();

                        // 🔥 Gson converts Firestore Map to JSON
                        Gson gson = new Gson();
                        String hashtagsJson = gson.toJson(documentSnapshot.getData());

                        // 🔥 Return to DashboardActivity
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("niche", niche);
                        resultIntent.putExtra("hashtagsJson", hashtagsJson);
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } else {
                        Toast.makeText(this, "❌ No data for " + niche, Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Firebase Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}
