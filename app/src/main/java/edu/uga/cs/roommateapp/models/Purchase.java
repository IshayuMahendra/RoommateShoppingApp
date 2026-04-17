package edu.uga.cs.roommateapp.models;

import java.util.Map;

public class Purchase {
    public String purchaseId;
    public String userId;
    public double totalPrice;
    public long timestamp;
    public Map<String, Item> items;

    public Purchase() {}

    public Purchase(String purchaseId, String userId, double totalPrice, long timestamp, Map<String, Item> items) {
        this.purchaseId = purchaseId;
        this.userId = userId;
        this.totalPrice = totalPrice;
        this.timestamp = timestamp;
        this.items = items;
    }
}