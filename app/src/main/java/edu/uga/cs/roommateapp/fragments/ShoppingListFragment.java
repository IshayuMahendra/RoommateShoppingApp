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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
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

public class ShoppingListFragment extends Fragment {

    private RecyclerView recyclerView;
    private ShoppingListAdapter adapter;
    private List<Item> itemList;
    private DatabaseReference databaseReference;
    private DatabaseReference basketReference;
    private FloatingActionButton fabAddItem;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_shopping_list, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewShoppingList);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        itemList = new ArrayList<>();
        adapter = new ShoppingListAdapter(itemList, this::showItemOptionsDialog);
        recyclerView.setAdapter(adapter);

        fabAddItem = view.findViewById(R.id.fabAddItem);
        fabAddItem.setOnClickListener(v -> showAddItemDialog());

        databaseReference = FirebaseDatabase.getInstance().getReference("shopping_list");
        basketReference = FirebaseDatabase.getInstance().getReference("basket");

        databaseReference.addValueEventListener(new ValueEventListener() {
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
                Toast.makeText(getContext(), "Failed to load items.", Toast.LENGTH_SHORT).show();
            }
        });

        return view;
    }

    private void showAddItemDialog() {
        showEditDialog(null);
    }

    private void showItemOptionsDialog(Item item) {
        String[] options = {"Add to Basket", "Edit", "Delete"};
        new AlertDialog.Builder(getContext())
                .setTitle(item.name)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) { // Add to Basket
                        addToBasket(item);
                    } else if (which == 1) { // Edit
                        showEditDialog(item);
                    } else if (which == 2) { // Delete
                        deleteItem(item);
                    }
                })
                .show();
    }

    private void showEditDialog(@Nullable Item item) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        View view = inflater.inflate(R.layout.dialog_add_item, null);

        EditText etItemName = view.findViewById(R.id.etItemName);
        EditText etItemQuantity = view.findViewById(R.id.etItemQuantity);

        if (item != null) {
            etItemName.setText(item.name);
            etItemQuantity.setText(String.valueOf(item.quantity));
        }

        String title = (item == null) ? "Add Item" : "Edit Item";
        String positiveButton = (item == null) ? "Add" : "Update";

        new AlertDialog.Builder(getContext())
                .setTitle(title)
                .setView(view)
                .setPositiveButton(positiveButton, (dialog, which) -> {
                    String name = etItemName.getText().toString().trim();
                    String quantityStr = etItemQuantity.getText().toString().trim();
                    int quantity = quantityStr.isEmpty() ? 1 : Integer.parseInt(quantityStr);

                    if (!name.isEmpty()) {
                        if (item == null) {
                            addItemToFirebase(name, quantity);
                        } else {
                            updateItemInFirebase(item.itemId, name, quantity);
                        }
                    } else {
                        Toast.makeText(getContext(), "Item name cannot be empty", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void addItemToFirebase(String name, int quantity) {
        String itemId = databaseReference.push().getKey();
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getEmail() : "anonymous";
        Item item = new Item(itemId, name, quantity, userId);

        if (itemId != null) {
            databaseReference.child(itemId).setValue(item);
        }
    }

    private void updateItemInFirebase(String itemId, String name, int quantity) {
        databaseReference.child(itemId).child("name").setValue(name);
        databaseReference.child(itemId).child("quantity").setValue(quantity);
    }

    private void deleteItem(Item item) {
        databaseReference.child(item.itemId).removeValue()
                .addOnSuccessListener(aVoid -> Toast.makeText(getContext(), "Item deleted", Toast.LENGTH_SHORT).show());
    }

    private void addToBasket(Item item) {
        basketReference.child(item.itemId).setValue(item)
                .addOnSuccessListener(aVoid -> {
                    databaseReference.child(item.itemId).removeValue();
                    Toast.makeText(getContext(), "Item added to basket", Toast.LENGTH_SHORT).show();
                });
    }
}
