package com.example.smartpantrymanager.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartpantrymanager.R;
import com.example.smartpantrymanager.model.PantryItem;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PantryAdapter extends RecyclerView.Adapter<PantryAdapter.PantryViewHolder> {
    private final List<PantryItem> items = new ArrayList<>();
    private final OnItemClickListener clickListener;

    public interface OnItemClickListener {
        void onItemClick(PantryItem item);
        void onDeleteClick(PantryItem item);
    }

    public PantryAdapter(OnItemClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void setItems(List<PantryItem> pantryItems) {
        items.clear();
        if (pantryItems != null) {
            items.addAll(pantryItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PantryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pantry, parent, false);
        return new PantryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PantryViewHolder holder, int position) {
        PantryItem item = items.get(position);
        Context context = holder.itemView.getContext();

        String displayName = item.getDisplayName();
        holder.nameText.setText(displayName);
        holder.quantityText.setText(String.format(Locale.getDefault(), "%.1f %s", item.getQuantity(), item.getUnit()));

        // Initial letter badge
        if (displayName != null && !displayName.trim().isEmpty()) {
            holder.initialText.setText(displayName.trim().substring(0, 1).toUpperCase(Locale.getDefault()));
        } else {
            holder.initialText.setText("P");
        }

        String expiry = item.getExpiryDate();
        if (expiry != null && !expiry.trim().isEmpty()) {
            try {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                Date expiryDate = dateFormat.parse(expiry);
                Date now = new Date();

                if (expiryDate != null) {
                    long diffInMs = expiryDate.getTime() - now.getTime();
                    long daysRemaining = diffInMs / (1000 * 60 * 60 * 24);

                    if (daysRemaining < 0) {
                        long pastDays = Math.abs(daysRemaining);
                        holder.expiryText.setText("Expired • " + pastDays + (pastDays == 1 ? " day ago" : " days ago"));
                        holder.expiryText.setBackgroundResource(R.drawable.bg_status_expired);
                        holder.expiryText.setTextColor(ContextCompat.getColor(context, R.color.status_expired_text));
                    } else if (daysRemaining <= 3) {
                        holder.expiryText.setText("Expiring • " + daysRemaining + (daysRemaining == 1 ? " day left" : " days left"));
                        holder.expiryText.setBackgroundResource(R.drawable.bg_status_expiring);
                        holder.expiryText.setTextColor(ContextCompat.getColor(context, R.color.status_expiring_text));
                    } else {
                        holder.expiryText.setText("Fresh • " + daysRemaining + " days left");
                        holder.expiryText.setBackgroundResource(R.drawable.bg_status_fresh);
                        holder.expiryText.setTextColor(ContextCompat.getColor(context, R.color.status_fresh_text));
                    }
                }
            } catch (ParseException e) {
                holder.expiryText.setText("Expiry: " + expiry);
                holder.expiryText.setBackgroundResource(R.drawable.bg_status_fresh);
                holder.expiryText.setTextColor(ContextCompat.getColor(context, R.color.status_fresh_text));
            }
        } else {
            holder.expiryText.setText("No expiry date");
            holder.expiryText.setBackgroundResource(R.drawable.bg_status_fresh);
            holder.expiryText.setTextColor(ContextCompat.getColor(context, R.color.text_muted));
        }

        holder.itemView.setOnClickListener(v -> clickListener.onItemClick(item));
        holder.deleteButton.setOnClickListener(v -> clickListener.onDeleteClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class PantryViewHolder extends RecyclerView.ViewHolder {
        final TextView initialText;
        final TextView nameText;
        final TextView quantityText;
        final TextView expiryText;
        final ImageButton deleteButton;

        PantryViewHolder(@NonNull View itemView) {
            super(itemView);
            initialText = itemView.findViewById(R.id.pantryItemInitial);
            nameText = itemView.findViewById(R.id.pantryItemName);
            quantityText = itemView.findViewById(R.id.pantryItemQuantity);
            expiryText = itemView.findViewById(R.id.pantryItemExpiry);
            deleteButton = itemView.findViewById(R.id.deleteIngredientButton);
        }
    }
}
