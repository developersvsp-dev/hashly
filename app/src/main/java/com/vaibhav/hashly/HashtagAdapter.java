package com.vaibhav.hashly;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class HashtagAdapter extends RecyclerView.Adapter<HashtagAdapter.HashtagViewHolder> {

    private ArrayList<String> hashtags;

    public HashtagAdapter(ArrayList<String> hashtags) {
        this.hashtags = hashtags;
    }

    @NonNull
    @Override
    public HashtagViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new HashtagViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HashtagViewHolder holder, int position) {
        holder.bind(hashtags.get(position));
    }

    @Override
    public int getItemCount() {
        return hashtags.size();
    }

    static class HashtagViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public HashtagViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }

        public void bind(String hashtag) {
            textView.setText(hashtag);
            textView.setPadding(32, 24, 32, 24);
            textView.setTextSize(16);
        }
    }
}
