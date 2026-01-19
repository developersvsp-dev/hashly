package com.vaibhav.hashly;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

public class HooksDisplayActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private HooksAdapter adapter;
    private ArrayList<String> hooksList;
    private TextView titleText;
    private AutoCompleteTextView autoCompleteNiche;
    private FirebaseFirestore db;
    private String currentNiche = "trending";

    // 🔥 INTERSTITIAL AD
    private InterstitialAd interstitialAd;
    private boolean adShownForCurrentNiche = false;

    // 🔥 AD LIMITS (3 ads max per day)
    private static final int MAX_ADS_PER_DAY = 3;
    private static final String PREFS_NAME = "ad_prefs";
    private static final String KEY_ADS_COUNT = "ads_count";
    private static final String KEY_LAST_RESET_DATE = "last_reset_date";

    // 🔥 ALL 16 NICHES
    private final String[] nicheDisplayNames = {
            "Trending", "Beauty", "Business", "Cooking", "Crypto",
            "Education", "Fashion", "Finance", "Fitness", "Food",
            "Gaming", "Motivation", "Parenting", "Pets", "Tech", "Travel"
    };

    private final String[] nicheIds = {
            "trending", "beauty", "business", "cooking", "crypto",
            "education", "fashion", "finance", "fitness", "food",
            "gaming", "motivation", "parenting", "pets", "tech", "travel"
    };

    private static final String AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"; // Test ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hooks_display);

        initViews();
        setupRecyclerView();
        setupDropdown();
        loadInterstitialAd();
        loadHooks("trending");
    }

    private void initViews() {
        db = FirebaseFirestore.getInstance();
        recyclerView = findViewById(R.id.recyclerHooks);
        titleText = findViewById(R.id.hooksTitle);
        autoCompleteNiche = findViewById(R.id.autoCompleteNiche);
        hooksList = new ArrayList<>();
        titleText.setText("🔥 TRENDING HOOKS");
    }

    private void setupRecyclerView() {
        adapter = new HooksAdapter(hooksList);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupDropdown() {
        ArrayAdapter<String> adapterDropdown = new ArrayAdapter<>(
                this, android.R.layout.simple_dropdown_item_1line, nicheDisplayNames);

        autoCompleteNiche.setAdapter(adapterDropdown);
        autoCompleteNiche.setText("Trending", false);
        autoCompleteNiche.setOnClickListener(v -> autoCompleteNiche.showDropDown());

        autoCompleteNiche.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedNiche = nicheDisplayNames[position];
                String newNicheId = nicheIds[position];

                // 🔥 CHECK AD LIMITS FIRST
                if (!canShowAd(newNicheId)) {
                    updateNiche(newNicheId, selectedNiche);
                    return;
                }

                // 🔥 Show ad if ready
                if (interstitialAd != null && !adShownForCurrentNiche) {
                    showInterstitialAd(newNicheId, selectedNiche);
                } else {
                    updateNiche(newNicheId, selectedNiche);
                }
            }
        });
    }

    // 🔥 SMART AD LIMITS
    private boolean canShowAd(String newNicheId) {
        // 1. Must be different niche
        if (newNicheId.equals(currentNiche)) {
            return false;
        }

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // 2. Check daily limit
        int adsCount = prefs.getInt(KEY_ADS_COUNT, 0);
        int lastResetDay = prefs.getInt(KEY_LAST_RESET_DATE, 0);

        Calendar calendar = Calendar.getInstance();
        int today = calendar.get(Calendar.DAY_OF_YEAR);

        // 3. Reset counter if new day
        if (today != lastResetDay) {
            adsCount = 0;
            prefs.edit()
                    .putInt(KEY_ADS_COUNT, 0)
                    .putInt(KEY_LAST_RESET_DATE, today)
                    .apply();
        }

        // 4. Max 3 ads per day
        if (adsCount >= MAX_ADS_PER_DAY) {
            Log.d("AdMob", "Daily ad limit reached: " + adsCount + "/3");
            return false;
        }

        return true;
    }

    private void incrementAdCount() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        int adsCount = prefs.getInt(KEY_ADS_COUNT, 0);
        Calendar calendar = Calendar.getInstance();
        int today = calendar.get(Calendar.DAY_OF_YEAR);

        prefs.edit()
                .putInt(KEY_ADS_COUNT, adsCount + 1)
                .putInt(KEY_LAST_RESET_DATE, today)
                .apply();

        Log.d("AdMob", "Ad shown! Total today: " + (adsCount + 1) + "/3");
    }

    // 🔥 INTERSTITIAL AD METHODS
    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, AD_UNIT_ID, adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(InterstitialAd ad) {
                        interstitialAd = ad;
                        Log.d("AdMob", "Interstitial ad loaded successfully");
                    }

                    @Override
                    public void onAdFailedToLoad(LoadAdError loadAdError) {
                        Log.d("AdMob", "Interstitial ad failed: " + loadAdError.getMessage());
                        interstitialAd = null;
                    }
                });
    }

    private void showInterstitialAd(String newNicheId, String selectedNiche) {
        adShownForCurrentNiche = true;
        incrementAdCount(); // 🔥 Track ad count

        interstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                Log.d("AdMob", "Ad dismissed - loading niche");
                interstitialAd = null;
                loadInterstitialAd(); // Preload next ad
                updateNiche(newNicheId, selectedNiche);
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                Log.d("AdMob", "Ad failed to show: " + adError.getMessage());
                interstitialAd = null;
                loadInterstitialAd();
                adShownForCurrentNiche = false; // Allow retry
                updateNiche(newNicheId, selectedNiche);
            }

            @Override
            public void onAdShowedFullScreenContent() {
                Log.d("AdMob", "Ad showed fullscreen");
            }
        });

        interstitialAd.show(this);
    }

    private void updateNiche(String newNicheId, String selectedNiche) {
        currentNiche = newNicheId;
        adShownForCurrentNiche = false;
        Log.d("Hooks", "Selected: " + selectedNiche + " (" + currentNiche + ")");
        titleText.setText("🔥 " + selectedNiche.toUpperCase() + " HOOKS");
        loadHooks(currentNiche);
    }

    private void loadHooks(String niche) {
        Log.d("Hooks", "Loading niche: " + niche);

        db.collection("hooks").document(niche)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    hooksList.clear();

                    if (documentSnapshot.exists()) {
                        Map<String, Object> data = documentSnapshot.getData();
                        if (data != null) {
                            for (Map.Entry<String, Object> entry : data.entrySet()) {
                                String key = entry.getKey();
                                String hook = entry.getValue().toString();
                                if (hook != null && !hook.trim().isEmpty() &&
                                        !key.startsWith("__") && !key.equals("id")) {
                                    hooksList.add(hook);
                                }
                            }
                        }
                    }

                    final int count = hooksList.size();
                    Log.d("Hooks", niche + " loaded: " + count + " hooks");

                    runOnUiThread(() -> {
                        adapter.notifyDataSetChanged();
                        if (count == 0) {
                            titleText.setText("❌ " + niche.toUpperCase() + " - No hooks found");
                        } else {
                            titleText.setText("🔥 " + niche.toUpperCase() + " HOOKS (" + count + ")");
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("Hooks", "Error loading " + niche + ": " + e.getMessage());
                    runOnUiThread(() -> {
                        titleText.setText("❌ " + niche.toUpperCase() + " - Error loading");
                        adapter.notifyDataSetChanged();
                    });
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (interstitialAd != null) {
            interstitialAd = null;
        }
    }
}
