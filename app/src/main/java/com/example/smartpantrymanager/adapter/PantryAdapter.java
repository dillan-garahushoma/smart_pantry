package com.example.smartpantrymanager.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
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
        holder.nameText.setText(item.getDisplayName());
        holder.quantityText.setText(String.format(Locale.getDefault(), "%.1f %s", item.getQuantity(), item.getUnit()));

        String expiry = item.getExpiryDate();
        if (expiry != null && !expiry.trim().isEmpty()) {
            holder.expiryText.setText("Expiry: " + expiry);
            holder.expiryText.setVisibility(View.VISIBLE);
            holder.expiryText.setTextColor(getExpiryColor(holder.itemView.getContext(), expiry));
        } else {
            holder.expiryText.setText("No expiry date");
            holder.expiryText.setVisibility(View.VISIBLE);
            holder.expiryText.setTextColor(holder.itemView.getContext().getColor(R.color.charcoal));
        }

        holder.itemView.setOnClickListener(v -> clickListener.onItemClick(item));
        holder.deleteButton.setOnClickListener(v -> clickListener.onDeleteClick(item));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class PantryViewHolder extends RecyclerView.ViewHolder {
        final TextView nameText;
        final TextView quantityText;
        final TextView expiryText;
        final ImageButton deleteButton;

        PantryViewHolder(@NonNull View itemView) {
            super(itemView);
            nameText = itemView.findViewById(R.id.pantryItemName);
            quantityText = itemView.findViewById(R.id.pantryItemQuantity);
            expiryText = itemView.findViewById(R.id.pantryItemExpiry);
            deleteButton = itemView.findViewById(R.id.deleteIngredientButton);
        }
    }

    private int getExpiryColor(android.content.Context context, String expiryDate) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date expiry = dateFormat.parse(expiryDate);
            Date now = new Date();
            long diffInMs = expiry.getTime() - now.getTime();
            long daysRemaining = diffInMs / (1000 * 60 * 60 * 24);
            if (daysRemaining <= 3 && daysRemaining >= 0) {
                return context.getColor(R.color.amber);
            }
            if (daysRemaining < 0) {
                return context.getColor(R.color.soft_red);
            }
        } catch (ParseException e) {
            return context.getColor(R.color.charcoal);
        }
        return context.getColor(R.color.charcoal);
    }
}
