package edu.uga.cs.roommateapp.models;

public class Item {
    public String itemId;
    public String name;
    public int quantity;
    public String addedBy;

    public Item() {}

    public Item(String itemId, String name, int quantity, String addedBy) {
        this.itemId = itemId;
        this.name = name;
        this.quantity = quantity;
        this.addedBy = addedBy;
    }
}