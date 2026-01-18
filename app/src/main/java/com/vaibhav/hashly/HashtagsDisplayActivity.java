package com.vaibhav.hashly;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.Map;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class HashtagsDisplayActivity extends AppCompatActivity {
    private TextView tvNiche, tvTrendingHashtags;
    private ImageButton btnCopyHashtags;
    private RecyclerView rvHashtags;
    private FirebaseFirestore db;
    private ReelAdapter adapter;
    private ArrayList<Reel> reelList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hashtags_display);

        db = FirebaseFirestore.getInstance();

        // 🔥 SAFE VIEW INITIALIZATION
        initViews();
        setupRecyclerView();
        setupCopyButton();

        // 🔥 LOAD NICHE DATA
        String niche = getIntent().getStringExtra("niche");
        Log.d("HashtagsDebug", "🚨 NICHE FROM INTENT: '" + niche + "'");

        if (niche != null && !niche.equals("Select Niche")) {
            tvNiche.setText(niche.toUpperCase());
            fetchReels(niche);
        } else {
            Log.e("HashtagsDebug", "❌ INVALID NICHE: " + niche);
            showFallbackHashtags(niche);
            Toast.makeText(this, "❌ Invalid niche", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void initViews() {
        tvNiche = findViewById(R.id.tvNiche);
        tvTrendingHashtags = findViewById(R.id.tvTrendingHashtags);
        btnCopyHashtags = findViewById(R.id.btnCopyHashtags);
        rvHashtags = findViewById(R.id.rvHashtags);

        Log.d("HashtagsDebug", "✅ Views initialized");
    }

    private void setupRecyclerView() {
        reelList = new ArrayList<>();
        adapter = new ReelAdapter(reelList, this);
        rvHashtags.setLayoutManager(new LinearLayoutManager(this));
        rvHashtags.setAdapter(adapter);

        int spacing = getResources().getDimensionPixelOffset(R.dimen.item_spacing);
        rvHashtags.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                       @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.bottom = spacing;
                if (parent.getChildAdapterPosition(view) == 0) {
                    outRect.top = spacing;
                }
            }
        });
    }

    private void setupCopyButton() {
        btnCopyHashtags.setOnClickListener(v -> {
            String hashtagsText = tvTrendingHashtags.getText().toString();
            if (!hashtagsText.trim().isEmpty()) {
                copyToClipboard(hashtagsText);
            } else {
                Toast.makeText(this, "No hashtags to copy", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchReels(String niche) {
        Log.d("HashtagsDebug", "🔥 FETCHING: trends/hashly/niches/" + niche);

        db.collection("trends").document("hashly").collection("niches")
                .document(niche)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d("HashtagsDebug", "📄 Document exists: " + documentSnapshot.exists());
                    Log.d("HashtagsDebug", "🔍 RAW DATA: " + documentSnapshot.getData());

                    if (documentSnapshot.exists()) {
                        handleSuccess(documentSnapshot, niche);
                    } else {
                        handleNoDocument(niche);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("HashtagsDebug", "❌ FIREBASE ERROR: " + e.getMessage());
                    handleError(niche);
                });
    }

    private void handleSuccess(com.google.firebase.firestore.DocumentSnapshot documentSnapshot, String niche) {
        reelList.clear();

        // 🔥 CRITICAL: ALWAYS SHOW HASHTAGS (FIXED!)
        String trendingHashtags = getTrendingHashtags(documentSnapshot, niche);

        // 🔥 WHITE + BOLD MAGIC - GUARANTEED TO SHOW!
        SpannableStringBuilder whiteBoldHashtags = makeWhiteBold(trendingHashtags);
        tvTrendingHashtags.setText(whiteBoldHashtags);
        Log.d("HashtagsDebug", "🎨 TOP HASHTAGS SET: " + trendingHashtags);

        // 🔥 LOAD REELS WITH FIXED URLS
        loadReels(documentSnapshot);

        adapter.notifyDataSetChanged();
        Toast.makeText(this, "✅ " + niche.toUpperCase() + " loaded!", Toast.LENGTH_SHORT).show();
    }

    private String getTrendingHashtags(com.google.firebase.firestore.DocumentSnapshot documentSnapshot, String niche) {
        Object topHashtagsObj = documentSnapshot.get("hashtags");
        String trendingHashtags = "";

        if (topHashtagsObj instanceof ArrayList) {
            @SuppressWarnings("unchecked")
            ArrayList<?> topHashtagsArray = (ArrayList<?>) topHashtagsObj;
            trendingHashtags = extractHashtags(topHashtagsArray);
            Log.d("HashtagsDebug", "✅ EXTRACTED: '" + trendingHashtags + "'");

            // 🔥 HARDCODE DETECTOR
            if (trendingHashtags.toLowerCase().contains("parenting") &&
                    !niche.toLowerCase().contains("parent")) {
                Log.e("HashtagsDebug", "🚨 HARDCODE DETECTED! Using fallback");
                trendingHashtags = createFallbackHashtags(niche);
            }
        }

        // 🔥 GUARANTEE: ALWAYS SHOW SOMETHING!
        if (trendingHashtags.trim().isEmpty()) {
            trendingHashtags = createFallbackHashtags(niche);
            Log.w("HashtagsDebug", "⚠️ EMPTY hashtags → Using fallback");
        }

        return trendingHashtags;
    }

    private String createFallbackHashtags(String niche) {
        return String.format("#%s #%smotivation #%stips #trending #fyp #%slife #%squotes",
                niche, niche, niche, niche, niche);
    }

    // 🔥 🔥 🔥 REEL URL FIX - DIRECT TO REEL!
    private void loadReels(com.google.firebase.firestore.DocumentSnapshot documentSnapshot) {
        Object reelsObj = documentSnapshot.get("reels");
        if (reelsObj instanceof ArrayList) {
            @SuppressWarnings("unchecked")
            ArrayList<?> reelsArray = (ArrayList<?>) reelsObj;
            int reelCount = 0;

            for (int i = 0; i < Math.min(reelsArray.size(), 15); i++) {
                Object reelObj = reelsArray.get(i);
                if (reelObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> reel = (Map<String, Object>) reelObj;

                    String hashtags = extractReelHashtags(reel);
                    String rawUrl = getStringSafe(reel, "url");
                    String fixedUrl = fixReelUrl(rawUrl);  // 🔥 FIXED URL!

                    Log.d("HashtagsDebug", "🔧 RAW URL: " + rawUrl + " → FIXED: " + fixedUrl);

                    Reel reelData = new Reel(
                            getStringSafe(reel, "img"),
                            getStringSafe(reel, "preview"),
                            fixedUrl,  // ✅ DIRECT REEL URL
                            getLongSafe(reel, "rank"),
                            hashtags
                    );
                    reelList.add(reelData);
                    reelCount++;
                }
            }
            Log.d("HashtagsDebug", "✅ Loaded " + reelCount + " reels with FIXED URLs");
        } else {
            // 🔥 FALLBACK REEL
            reelList.add(new Reel(null, "📱 No reels yet - Run scraper!", null, 0, ""));
            Log.w("HashtagsDebug", "⚠️ No reels data");
        }
    }

    // 🔥 URL FIXER - Converts profile → reel URLs
    private String fixReelUrl(String url) {
        if (url == null || url.isEmpty()) return "https://www.instagram.com/reel/demo/";

        Log.d("HashtagsDebug", "🔧 FIXING URL: " + url);

        // /p/C1234567890/ → /reel/C1234567890/
        if (url.contains("/p/")) {
            String fixed = url.replace("/p/", "/reel/");
            Log.d("HashtagsDebug", "✅ FIXED p/ → reel/: " + fixed);
            return fixed;
        }

        // Profile pages → Extract code → Make reel URL
        if (url.contains("instagram.com/") && !url.contains("/reel/")) {
            String[] parts = url.split("/");
            if (parts.length > 3) {
                String code = parts[parts.length - 2].replace("/", "");
                if (!code.isEmpty()) {
                    String reelUrl = "https://www.instagram.com/reel/" + code + "/";
                    Log.d("HashtagsDebug", "✅ FIXED profile → reel: " + reelUrl);
                    return reelUrl;
                }
            }
        }

        Log.d("HashtagsDebug", "✅ URL already good: " + url);
        return url;
    }

    private String extractReelHashtags(Map<String, Object> reel) {
        StringBuilder hashtags = new StringBuilder();
        Object hashtagsObj = reel.get("hashtags");
        if (hashtagsObj instanceof ArrayList) {
            ArrayList<?> hashtagsArray = (ArrayList<?>) hashtagsObj;
            for (Object tagObj : hashtagsArray) {
                if (tagObj instanceof String) {
                    hashtags.append("#").append(tagObj).append(" ");
                } else if (tagObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> tag = (Map<String, Object>) tagObj;
                    String tagName = getStringSafe(tag, "name");
                    hashtags.append("#").append(tagName).append(" ");
                }
            }
        }
        return hashtags.toString().trim();
    }

    private void handleNoDocument(String niche) {
        Log.e("HashtagsDebug", "❌ NO DOCUMENT for niche: " + niche);
        showFallbackHashtags(niche);
        reelList.clear();
        reelList.add(new Reel(null, "❌ Upload data for " + niche.toUpperCase(), null, 0, ""));
        adapter.notifyDataSetChanged();
        Toast.makeText(this, "❌ No data for " + niche + ". Run scraper!", Toast.LENGTH_LONG).show();
    }

    private void handleError(String niche) {
        showFallbackHashtags(niche);
        Toast.makeText(this, "❌ Network error - Using fallback", Toast.LENGTH_LONG).show();
    }

    private void showFallbackHashtags(String niche) {
        String fallback = createFallbackHashtags(niche);
        SpannableStringBuilder whiteBold = makeWhiteBold(fallback);
        tvTrendingHashtags.setText(whiteBold);
        Log.d("HashtagsDebug", "🔥 FALLBACK SHOWN: " + fallback);
    }

    // 🔥 WHITE + BOLD HASHTAGS MAGIC
    private SpannableStringBuilder makeWhiteBold(String hashtagsText) {
        SpannableStringBuilder ssb = new SpannableStringBuilder(hashtagsText);
        Pattern pattern = Pattern.compile("#\\w+");
        Matcher matcher = pattern.matcher(hashtagsText);

        while (matcher.find()) {
            // 🔥 WHITE COLOR + BOLD for BLUE BG
            ssb.setSpan(new ForegroundColorSpan(0xFFFFFFFF), // WHITE ✨
                    matcher.start(), matcher.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            // 🔥 BOLD TEXT
            ssb.setSpan(new StyleSpan(Typeface.BOLD),
                    matcher.start(), matcher.end(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return ssb;
    }

    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Hashly Hashtags", text);
        clipboard.setPrimaryClip(clip);

        btnCopyHashtags.setImageResource(android.R.drawable.ic_menu_save);
        btnCopyHashtags.postDelayed(() -> {
            btnCopyHashtags.setImageResource(R.drawable.ic_copy);
        }, 800);

        Toast.makeText(this, "📋 Copied " + text.split(" ").length + " hashtags!", Toast.LENGTH_SHORT).show();
        Log.d("HashtagsDebug", "✅ Copied: " + text);
    }

    private String getStringSafe(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return (value instanceof String) ? (String) value : "";
    }

    private long getLongSafe(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return (value instanceof Long) ? (Long) value : 0L;
    }

    private String extractHashtags(ArrayList<?> hashtagsArray) {
        StringBuilder sb = new StringBuilder();
        boolean firstTag = true;

        for (Object tagObj : hashtagsArray) {
            if (tagObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> tag = (Map<String, Object>) tagObj;
                String tagName = getStringSafe(tag, "name");

                // 🔥 CHECK IF ALREADY HAS # - DON'T ADD AGAIN
                if (!tagName.isEmpty()) {
                    String cleanTag = tagName.startsWith("#") ? tagName : "#" + tagName;
                    if (!firstTag) sb.append(" ");
                    sb.append(cleanTag);
                    firstTag = false;
                }
            } else if (tagObj instanceof String) {
                String tagName = (String) tagObj;
                String cleanTag = tagName.startsWith("#") ? tagName : "#" + tagName;
                if (!tagName.isEmpty()) {
                    if (!firstTag) sb.append(" ");
                    sb.append(cleanTag);
                    firstTag = false;
                }
            }
        }
        return sb.toString();
    }

}
