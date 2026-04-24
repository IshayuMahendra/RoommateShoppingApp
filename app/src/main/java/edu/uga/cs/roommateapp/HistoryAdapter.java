package edu.uga.cs.roommateapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import edu.uga.cs.roommateapp.models.Item;
import edu.uga.cs.roommateapp.models.Purchase;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<Purchase> purchaseList;

    public HistoryAdapter(List<Purchase> purchaseList) {
        this.purchaseList = purchaseList;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_purchase_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        Purchase purchase = purchaseList.get(position);

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());
        holder.tvPurchaseDate.setText("Date: " + sdf.format(new Date(purchase.timestamp)));
        holder.tvPurchasedBy.setText("Purchased by: " + purchase.userId);
        holder.tvTotalPrice.setText(String.format(Locale.getDefault(), "Total: $%.2f", purchase.totalPrice));

        StringBuilder itemsSummary = new StringBuilder("Items: ");
        if (purchase.items != null) {
            for (Item item : purchase.items.values()) {
                itemsSummary.append(item.name).append(" (x").append(item.quantity).append("), ");
            }
        }
        String summary = itemsSummary.toString();
        if (summary.endsWith(", ")) {
            summary = summary.substring(0, summary.length() - 2);
        }
        holder.tvItemsSummary.setText(summary);
    }

    @Override
    public int getItemCount() {
        return purchaseList.size();
    }

    public static class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvPurchaseDate, tvPurchasedBy, tvTotalPrice, tvItemsSummary;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPurchaseDate = itemView.findViewById(R.id.tvPurchaseDate);
            tvPurchasedBy = itemView.findViewById(R.id.tvPurchasedBy);
            tvTotalPrice = itemView.findViewById(R.id.tvTotalPrice);
            tvItemsSummary = itemView.findViewById(R.id.tvItemsSummary);
        }
    }
}
