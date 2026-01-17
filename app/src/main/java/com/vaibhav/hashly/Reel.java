package com.vaibhav.hashly;

public class Reel {
    public String imgUrl, preview, url, hashtags;
    public long rank;

    public Reel(String imgUrl, String preview, String url, long rank, String hashtags) {
        this.imgUrl = imgUrl;
        this.preview = preview;
        this.url = url;
        this.rank = rank;
        this.hashtags = hashtags;
    }

    // 🔥 NEW: Detect header items
    public boolean isHeader() {
        return preview != null && preview.contains("HASHTAGS");
    }
}
