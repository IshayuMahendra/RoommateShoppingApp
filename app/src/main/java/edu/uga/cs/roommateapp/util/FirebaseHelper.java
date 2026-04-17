package edu.uga.cs.roommateapp.util;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Map;

import edu.uga.cs.roommateapp.models.BasketItem;
import edu.uga.cs.roommateapp.models.Item;
import edu.uga.cs.roommateapp.models.Purchase;

public class FirebaseHelper {

    private static final DatabaseReference root =
            FirebaseDatabase.getInstance().getReference();

    // =========================
    // SHOPPING LIST
    // =========================

    public static void addItem(String name, int qty, String userId) {
        DatabaseReference ref = root.child("shoppingList");

        String id = ref.push().getKey();

        if (id == null) return;

        Item item = new Item(id, name, qty, userId);
        ref.child(id).setValue(item);
    }

    public static void updateItem(String itemId, String name, int qty) {
        DatabaseReference ref = root.child("shoppingList").child(itemId);

        ref.child("name").setValue(name);
        ref.child("quantity").setValue(qty);
    }

    public static void deleteItem(String itemId) {
        root.child("shoppingList")
                .child(itemId)
                .removeValue();
    }

    // =========================
    // BASKET
    // =========================

    public static void moveToBasket(Item item, String userId) {
        if (item == null || item.itemId == null) return;

        DatabaseReference basketRef =
                root.child("baskets").child(userId);

        BasketItem basketItem =
                new BasketItem(item.itemId, item.name, item.quantity);

        basketRef.child(item.itemId).setValue(basketItem);

        // remove from shopping list
        root.child("shoppingList")
                .child(item.itemId)
                .removeValue();
    }

    public static void removeFromBasket(BasketItem item, String userId) {
        if (item == null) return;

        // remove from basket
        root.child("baskets")
                .child(userId)
                .child(item.itemId)
                .removeValue();

        // add back to shopping list
        Item restored = new Item(
                item.itemId,
                item.name,
                item.quantity,
                userId
        );

        root.child("shoppingList")
                .child(item.itemId)
                .setValue(restored);
    }

    // =========================
    // CHECKOUT / PURCHASES
    // =========================

    public static void checkout(String userId, double total, Map<String, Item> items) {
        DatabaseReference ref = root.child("purchases");

        String id = ref.push().getKey();
        if (id == null) return;

        Purchase purchase = new Purchase(
                id,
                userId,
                total,
                System.currentTimeMillis(),
                items
        );

        ref.child(id).setValue(purchase);

        // clear basket after checkout
        root.child("baskets")
                .child(userId)
                .removeValue();
    }



    public static DatabaseReference getShoppingListRef() {
        return root.child("shoppingList");
    }

    public static DatabaseReference getBasketRef(String userId) {
        return root.child("baskets").child(userId);
    }

    public static DatabaseReference getPurchasesRef() {
        return root.child("purchases");
    }
}