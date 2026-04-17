package edu.uga.cs.roommateapp.models;

public class BasketItem {
    public String itemId;
    public String name;
    public int quantity;

    public BasketItem() {}

    public BasketItem(String itemId, String name, int quantity) {
        this.itemId = itemId;
        this.name = name;
        this.quantity = quantity;
    }
}