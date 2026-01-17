package com.vaibhav.hashly;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class ReelAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_HEADER = 0;
    private static final int VIEW_TYPE_REEL = 1;
    private ArrayList<Reel> reels;
    private Context context;

    public ReelAdapter(ArrayList<Reel> reels, Context context) {
        this.reels = reels;
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        return reels.get(position).isHeader() ? VIEW_TYPE_HEADER : VIEW_TYPE_REEL;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false);
            return new HeaderViewHolder(view);
        }
        View view = LayoutInflater.from(context).inflate(R.layout.item_reel, parent, false);
        return new ReelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(reels.get(position));
        } else {
            ((ReelViewHolder) holder).bind(reels.get(position));
        }
    }

    @Override
    public int getItemCount() {
        return reels.size();
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView text1;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
        }

        public void bind(Reel reel) {
            text1.setText(reel.preview);
            text1.setTextSize(22);
            text1.setPadding(32, 32, 32, 24);
            text1.setTypeface(null, Typeface.BOLD);
            text1.setTextColor(0xFF1DA1F2);
            text1.setBackgroundColor(0xFFF0F0F0);
        }
    }

    // 🔥 NO ImageView - TEXT ONLY!
    public static class ReelViewHolder extends RecyclerView.ViewHolder {
        TextView tvPreview, tvHashtags;

        public ReelViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPreview = itemView.findViewById(R.id.tvPreview);
            tvHashtags = itemView.findViewById(R.id.tvHashtags);
        }

        public void bind(Reel reel) {
            tvPreview.setText(reel.preview);
            tvHashtags.setText(reel.hashtags);

            // Tap card → Open Instagram
            itemView.setOnClickListener(v -> {
                if (reel.url != null && !reel.url.isEmpty()) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(reel.url));
                    itemView.getContext().startActivity(intent);
                }
            });
        }
    }
}
