package com.example.tradeup_app.auth.Helper;

import com.example.tradeup_app.auth.Domain.UserModel;

public class CurrentUser {
    private static UserModel currentUser;

    public static void setUser(UserModel user) {
        currentUser = user;
    }

    public static UserModel getUser() {
        return currentUser;
    }

    public static void clear() {
        currentUser = null;
    }
}
