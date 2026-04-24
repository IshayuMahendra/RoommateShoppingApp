package edu.uga.cs.roommateapp.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
import java.util.List;

import edu.uga.cs.roommateapp.R;
import edu.uga.cs.roommateapp.ShoppingListAdapter;
import edu.uga.cs.roommateapp.models.Item;

public class RecentlyPurchasedFragment extends Fragment {

    private RecyclerView recyclerView;
    private ShoppingListAdapter adapter;
    private List<Item> itemList;
    private DatabaseReference purchasedReference;
    private DatabaseReference shoppingListReference;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recently_purchased, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewRecentlyPurchased);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        itemList = new ArrayList<>();
        adapter = new ShoppingListAdapter(itemList, this::showBasketOptionsDialog);
        recyclerView.setAdapter(adapter);

        purchasedReference = FirebaseDatabase.getInstance().getReference("purchased_items");
        shoppingListReference = FirebaseDatabase.getInstance().getReference("shopping_list");

        purchasedReference.addValueEventListener(new ValueEventListener() {
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

        return view;
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
                    purchasedReference.child(item.itemId).removeValue();
                    Toast.makeText(getContext(), "Item returned to shopping list", Toast.LENGTH_SHORT).show();
                });
    }
}
