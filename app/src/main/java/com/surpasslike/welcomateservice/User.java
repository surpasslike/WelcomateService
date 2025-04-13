package com.surpasslike.welcomateservice;

public class User {
    private final String username;
    private final String account;
    private final String password;

    public User(String username, String account, String password) {
        this.username = username;
        this.account = account;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getAccount() {
        return account;
    }

    public String getPassword() {
        return password;
    }
}
