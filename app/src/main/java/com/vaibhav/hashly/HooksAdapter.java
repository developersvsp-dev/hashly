package com.vaibhav.hashly;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class HooksAdapter extends RecyclerView.Adapter<HooksAdapter.ViewHolder> {
    private ArrayList<String> hooks;

    public HooksAdapter(ArrayList<String> hooks) {
        this.hooks = hooks;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hook, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String hook = hooks.get(position);
        holder.bind(hook);  // 🔥 Enhanced bind method
    }

    @Override
    public int getItemCount() {
        return hooks.size();
    }

    // 🔥 Update data method
    public void updateHooks(ArrayList<String> newHooks) {
        this.hooks.clear();
        this.hooks.addAll(newHooks);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView hookText, hookLength;

        ViewHolder(View view) {
            super(view);
            hookText = view.findViewById(R.id.hookText);
            hookLength = view.findViewById(R.id.hookLength);
        }

        void bind(String hook) {
            hookText.setText(hook);

            // 🔥 Length indicator + color coding
            int length = hook.length();
            String lengthType;
            int color;
            if (length < 50) {
                lengthType = "Short";
                color = 0xFF4CAF50;  // Green
            } else if (length < 100) {
                lengthType = "Medium";
                color = 0xFFFF9800;  // Orange
            } else {
                lengthType = "Long";
                color = 0xFFF44336;  // Red
            }
            hookLength.setText(lengthType + " • " + length + " chars");
            hookLength.setTextColor(color);

            // 🔥 TAP ANYWHERE TO COPY
            itemView.setOnClickListener(v -> copyToClipboard(v.getContext(), hook));

            // 🔥 LONG PRESS FOR SHARE
            itemView.setOnLongClickListener(v -> {
                shareHook(v.getContext(), hook);
                return true;
            });
        }

        private void copyToClipboard(Context context, String text) {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Hook", text);
            clipboard.setPrimaryClip(clip);

            // 🔥 Smooth feedback
            itemView.animate()
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .setDuration(100)
                    .withEndAction(() -> itemView.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(100)
                            .start())
                    .start();

            Toast.makeText(context, "✅ Copied to clipboard!", Toast.LENGTH_SHORT).show();
        }

        private void shareHook(Context context, String text) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, text);
            context.startActivity(Intent.createChooser(shareIntent, "Share Hook"));
        }
    }
}
