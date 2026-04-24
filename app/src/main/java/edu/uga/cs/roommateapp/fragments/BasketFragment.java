package edu.uga.cs.roommateapp.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.uga.cs.roommateapp.R;
import edu.uga.cs.roommateapp.ShoppingListAdapter;
import edu.uga.cs.roommateapp.models.Item;
import edu.uga.cs.roommateapp.models.Purchase;

public class BasketFragment extends Fragment {

    private RecyclerView recyclerView;
    private ShoppingListAdapter adapter;
    private List<Item> itemList;
    private DatabaseReference basketReference;
    private DatabaseReference shoppingListReference;
    private DatabaseReference purchasesReference;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_basket, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewBasket);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        itemList = new ArrayList<>();
        adapter = new ShoppingListAdapter(itemList, this::showBasketOptionsDialog);
        recyclerView.setAdapter(adapter);

        basketReference = FirebaseDatabase.getInstance().getReference("basket");
        shoppingListReference = FirebaseDatabase.getInstance().getReference("shopping_list");
        purchasesReference = FirebaseDatabase.getInstance().getReference("purchases");

        basketReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                itemList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Item item = dataSnapshot.getValue(Item.class);
                    if (item != null) {
                        item.itemId = dataSnapshot.getKey();
                        itemList.add(item);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });

        view.findViewById(R.id.btnSettleCost).setOnClickListener(v -> {
            if (itemList.isEmpty()) {
                Toast.makeText(getContext(), "Basket is empty!", Toast.LENGTH_SHORT).show();
            } else {
                showCheckoutDialog();
            }
        });

        return view;
    }

    private void showCheckoutDialog() {
        EditText etPrice = new EditText(getContext());
        etPrice.setHint("Total Price (e.g. 25.50)");
        etPrice.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);

        new AlertDialog.Builder(getContext())
                .setTitle("Checkout")
                .setMessage("Enter the total price for these items:")
                .setView(etPrice)
                .setPositiveButton("Buy", (dialog, which) -> {
                    String priceStr = etPrice.getText().toString().trim();
                    if (!priceStr.isEmpty()) {
                        try {
                            double price = Double.parseDouble(priceStr);
                            checkout(price);
                        } catch (NumberFormatException e) {
                            Toast.makeText(getContext(), "Invalid price format", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(), "Please enter a price", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void checkout(double totalPrice) {
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getEmail() : "anonymous";
        
        String purchaseId = purchasesReference.push().getKey();
        long timestamp = System.currentTimeMillis();

        Map<String, Item> itemsMap = new HashMap<>();
        for (Item item : itemList) {
            itemsMap.put(item.itemId, item);
        }

        Purchase purchase = new Purchase(purchaseId, userId, totalPrice, timestamp, itemsMap);

        if (purchaseId != null) {
            purchasesReference.child(purchaseId).setValue(purchase)
                    .addOnSuccessListener(aVoid -> {
                        basketReference.removeValue();
                        Toast.makeText(getContext(), "Purchase recorded!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Checkout failed.", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void showBasketOptionsDialog(Item item) {
        String[] options = {"Remove from Basket (Return to List)"};
        new AlertDialog.Builder(getContext())
                .setTitle(item.name)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        returnToList(item);
                    }
                })
                .show();
    }

    private void returnToList(Item item) {
        shoppingListReference.child(item.itemId).setValue(item)
                .addOnSuccessListener(aVoid -> {
                    basketReference.child(item.itemId).removeValue();
                    Toast.makeText(getContext(), "Item returned to shopping list", Toast.LENGTH_SHORT).show();
                });
    }
}
