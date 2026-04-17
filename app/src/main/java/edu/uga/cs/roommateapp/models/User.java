package edu.uga.cs.roommateapp.models;

public class User {
    public String userId;
    public String email;
    public String name;

    public User() {} // REQUIRED

    public User(String userId, String email, String name) {
        this.userId = userId;
        this.email = email;
        this.name = name;
    }
}