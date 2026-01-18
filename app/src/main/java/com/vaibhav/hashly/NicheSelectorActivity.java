package com.vaibhav.hashly;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NicheSelectorActivity extends AppCompatActivity {
    private Spinner nicheSpinner;
    private FirebaseFirestore db;
    private InterstitialAd interstitialAd;

    // 🔥 AD LIMITING SYSTEM
    private int nicheCount = 0;
    private static final int MAX_ADS_PER_DAY = 2;
    private static final String PREFS_NAME = "HashlyPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_niche_selector);

        db = FirebaseFirestore.getInstance();
        nicheSpinner = findViewById(R.id.nicheSpinner);

        // 🔥 LOAD COUNTER + PRELOAD AD
        loadNicheCount();
        loadInterstitialAd();

        // 🔥 SPINNER SETUP
        String[] niches = {
                "Select Niche",
                "beauty", "business", "cars", "cartoon", "comedy", "cooking",
                "dance", "fitness", "food", "gaming", "luxury", "memes",
                "music", "parenting", "pets", "photography", "quotes",
                "sports", "technology", "travel", "wellness"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, niches);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        nicheSpinner.setAdapter(adapter);
        nicheSpinner.setSelection(0);

        // 🔥 BACK BUTTON
        findViewById(R.id.btnBack).setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        // 🔥 SPINNER LISTENER
        nicheSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedNiche = parent.getItemAtPosition(position).toString();
                if (selectedNiche.equals("Select Niche") || position == 0) {
                    return;
                }

                // 🔥 INCREMENT COUNTER
                nicheCount++;
                saveNicheCount();

                fetchHashtagsForNiche(selectedNiche);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    // 🔥 LOAD INTERSTITIAL AD
    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, "ca-app-pub-3940256099942544/1033173712", adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(InterstitialAd ad) {
                        interstitialAd = ad;
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        interstitialAd = null;
                    }
                });
    }

    // 🔥 FETCH HASHTAGS FROM FIREBASE
    private void fetchHashtagsForNiche(String niche) {
        if (niche.equals("Select Niche")) return;

        Toast.makeText(this, "🔥 Loading " + niche + " hashtags...", Toast.LENGTH_SHORT).show();

        db.collection("trends")
                .document("hashly")
                .collection("niches")
                .document(niche)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Toast.makeText(this, "✅ " + niche + " hashtags loaded!", Toast.LENGTH_SHORT).show();

                        Gson gson = new Gson();
                        String hashtagsJson = gson.toJson(documentSnapshot.getData());

                        // 🔥 2 ADS MAX: 1st & 2nd niche → AD, 3rd+ → NO AD
                        if (nicheCount <= MAX_ADS_PER_DAY && interstitialAd != null) {
                            showInterstitialAd(hashtagsJson, niche);
                        } else {
                            goToResults(hashtagsJson, niche);
                        }
                    } else {
                        Toast.makeText(this, "❌ No data for " + niche, Toast.LENGTH_LONG).show();
                        goToResults(null, niche);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "❌ Firebase Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    goToResults(null, niche);
                });
    }

    // 🔥 SHOW AD → THEN RESULTS
    private void showInterstitialAd(String hashtagsJson, String niche) {
        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                goToResults(hashtagsJson, niche);
                loadInterstitialAd(); // Preload next
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                goToResults(hashtagsJson, niche);
            }
        });
        interstitialAd.show(this);
    }

    // 🔥 RETURN RESULTS TO DASHBOARD
    private void goToResults(String hashtagsJson, String niche) {
        Intent resultIntent = new Intent();
        if (hashtagsJson != null) {
            resultIntent.putExtra("niche", niche);
            resultIntent.putExtra("hashtagsJson", hashtagsJson);
            setResult(RESULT_OK, resultIntent);
        } else {
            setResult(RESULT_CANCELED, resultIntent);
        }
        finish();
    }

    // 🔥 SAVE DAILY COUNTER
    private void saveNicheCount() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String today = getTodayDate();
        prefs.edit()
                .putInt("nicheCount_" + today, nicheCount)
                .apply();
    }

    // 🔥 LOAD DAILY COUNTER (Resets at midnight)
    private void loadNicheCount() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String today = getTodayDate();
        nicheCount = prefs.getInt("nicheCount_" + today, 0);
    }

    // 🔥 TODAY'S DATE (YYYY-MM-DD)
    private String getTodayDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(new Date());
    }
}
