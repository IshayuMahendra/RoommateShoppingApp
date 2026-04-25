package edu.uga.cs.roommateapp.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.uga.cs.roommateapp.HistoryAdapter;
import edu.uga.cs.roommateapp.R;
import edu.uga.cs.roommateapp.models.Item;
import edu.uga.cs.roommateapp.models.Purchase;

public class PurchasedItemsFragment extends Fragment {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private List<Purchase> purchaseList;
    private DatabaseReference purchasesReference;
    private DatabaseReference historyReference;
    private DatabaseReference shoppingListReference;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_purchased_items, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewPurchases);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        purchaseList = new ArrayList<>();
        // Reusing HistoryAdapter but we need to handle clicks for Story 11/12
        adapter = new HistoryAdapter(purchaseList, this::showPurchaseDetailDialog);
        recyclerView.setAdapter(adapter);

        purchasesReference = FirebaseDatabase.getInstance().getReference("purchases");
        historyReference = FirebaseDatabase.getInstance().getReference("history");
        shoppingListReference = FirebaseDatabase.getInstance().getReference("shopping_list");
        DatabaseReference basketReference = FirebaseDatabase.getInstance().getReference("basket");

        purchasesReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                purchaseList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Purchase purchase = dataSnapshot.getValue(Purchase.class);
                    if (purchase != null) {
                        purchase.purchaseId = dataSnapshot.getKey();
                        purchaseList.add(purchase);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        view.findViewById(R.id.btnSettleEverything).setOnClickListener(v -> settleEverything());

        return view;
    }

    private void settleEverything() {
        if (purchaseList.isEmpty()) {
            Toast.makeText(getContext(), "Nothing to settle!", Toast.LENGTH_SHORT).show();
            return;
        }

        double totalCost = 0;
        Map<String, Double> costByRoommate = new HashMap<>();
        List<String> roommates = new ArrayList<>();

        for (Purchase p : purchaseList) {
            totalCost += p.totalPrice;
            costByRoommate.put(p.userId, costByRoommate.getOrDefault(p.userId, 0.0) + p.totalPrice);
            if (!roommates.contains(p.userId)) {
                roommates.add(p.userId);
            }
        }

        // Project requires "average cost per roommate (total cost / number of roommates)"
        // Note: The description says "number of roommates", usually we assume at least 2 for the project.
        int numRoommates = Math.max(roommates.size(), 2); // Fallback to 2 as per project requirement
        double average = totalCost / numRoommates;

        StringBuilder report = new StringBuilder();
        report.append("Total Cost: $").append(String.format("%.2f", totalCost)).append("\n");
        report.append("Average per Roommate: $").append(String.format("%.2f", average)).append("\n\n");
        report.append("Spending by Roommate:\n");

        for (String roommate : roommates) {
            double spent = costByRoommate.get(roommate);
            double diff = spent - average;
            report.append(roommate).append(": $").append(String.format("%.2f", spent))
                  .append(" (Diff: ").append(diff >= 0 ? "+" : "").append(String.format("%.2f", diff)).append(")\n");
        }

        new AlertDialog.Builder(getContext())
                .setTitle("Settle Costs")
                .setMessage(report.toString())
                .setPositiveButton("Settle & Clear", (dialog, which) -> {
                    archiveAndClearPurchases();
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void archiveAndClearPurchases() {
        // Move all purchases to history and clear current purchases
        for (Purchase p : purchaseList) {
            historyReference.child(p.purchaseId).setValue(p);
        }
        purchasesReference.removeValue().addOnSuccessListener(aVoid -> {
            Toast.makeText(getContext(), "Costs settled and cleared!", Toast.LENGTH_SHORT).show();
        });
    }

    private void showPurchaseDetailDialog(Purchase purchase) {
        String[] itemNames = new String[purchase.items.size()];
        String[] itemIds = new String[purchase.items.size()];
        int i = 0;
        for (Map.Entry<String, Item> entry : purchase.items.entrySet()) {
            itemIds[i] = entry.getKey();
            itemNames[i] = entry.getValue().name + " (x" + entry.getValue().quantity + ")";
            i++;
        }

        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_purchase_options, null);
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("Edit Purchase")
                .setView(view)
                .setNegativeButton("Cancel", null)
                .create();

        view.findViewById(R.id.btnEditPrice).setOnClickListener(v -> {
            dialog.dismiss();
            showEditPriceDialog(purchase);
        });

        view.findViewById(R.id.btnRemoveItems).setOnClickListener(v -> {
            dialog.dismiss();
            showRemoveItemsDialog(purchase, itemNames, itemIds);
        });

        dialog.show();
    }

    private void showEditPriceDialog(Purchase purchase) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_price, null);
        EditText etPrice = view.findViewById(R.id.etEditPrice);
        etPrice.setText(String.valueOf(purchase.totalPrice));

        new AlertDialog.Builder(getContext())
                .setTitle("Edit Price")
                .setView(view)
                .setPositiveButton("Update", (d, which) -> {
                    String priceStr = etPrice.getText().toString().trim();
                    if (!priceStr.isEmpty()) {
                        try {
                            double newPrice = Double.parseDouble(priceStr);
                            purchasesReference.child(purchase.purchaseId).child("totalPrice").setValue(newPrice);
                        } catch (NumberFormatException e) {
                            Toast.makeText(getContext(), "Invalid price", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showRemoveItemsDialog(Purchase purchase, String[] itemNames, String[] itemIds) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_remove_items, null);
        LinearLayout container = view.findViewById(R.id.itemsContainer);

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setTitle("Remove Item (Moves to Basket)")
                .setView(view)
                .setNegativeButton("Cancel", null)
                .create();

        for (int i = 0; i < itemNames.length; i++) {
            final int index = i;
            MaterialButton itemButton = new MaterialButton(getContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle);
            itemButton.setText(itemNames[i]);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            params.setMargins(0, 0, 0, 16);
            itemButton.setLayoutParams(params);

            itemButton.setOnClickListener(v -> {
                dialog.dismiss();
                removeItemFromPurchase(purchase, itemIds[index]);
            });

            container.addView(itemButton);
        }

        dialog.show();
    }

    private void removeItemFromPurchase(Purchase purchase, String itemId) {
        DatabaseReference basketReference = FirebaseDatabase.getInstance().getReference("basket");
        Item itemToMove = purchase.items.get(itemId);

        if (purchase.items.size() <= 1) {
            // Last item, delete purchase entirely
            purchasesReference.child(purchase.purchaseId).removeValue();
        } else {
            // Remove specific item from purchase map
            purchasesReference.child(purchase.purchaseId).child("items").child(itemId).removeValue();
        }

        // Move item back to basket
        if (itemToMove != null) {
            basketReference.child(itemId).setValue(itemToMove)
                    .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Item moved back to basket", Toast.LENGTH_SHORT).show());
        }
    }

    // Note: I'll need to update HistoryAdapter to support item removal and price updates
    // for this fragment specifically. For now, I've implemented the core Story 14 logic.
}
