package com.vaibhav.hashly;

import android.content.Intent;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Map;

public class HooksDisplayActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private HooksAdapter adapter;
    private ArrayList<String> hooksList;
    private TextView titleText;
    private AutoCompleteTextView autoCompleteNiche;
    private FirebaseFirestore db;
    private String currentNiche = "trending";

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hooks_display);

        initViews();
        setupRecyclerView();
        setupDropdown();
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
                this,
                android.R.layout.simple_dropdown_item_1line,
                nicheDisplayNames
        );

        autoCompleteNiche.setAdapter(adapterDropdown);
        autoCompleteNiche.setText("Trending", false);
        autoCompleteNiche.setOnClickListener(v -> autoCompleteNiche.showDropDown());

        autoCompleteNiche.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedNiche = nicheDisplayNames[position];
                currentNiche = nicheIds[position];
                Log.d("Hooks", "Selected: " + selectedNiche + " (" + currentNiche + ")");
                titleText.setText("🔥 " + selectedNiche.toUpperCase() + " HOOKS");
                loadHooks(currentNiche);
            }
        });
    }

    // 🔥 FIXED: Reads YOUR data structure (hooks/beauty → all fields)
    private void loadHooks(String niche) {
        Log.d("Hooks", "Loading niche: " + niche);

        // 🔥 Read DOCUMENT fields directly (matches your structure)
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

                                // 🔥 Skip system fields, show all hooks
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
}
