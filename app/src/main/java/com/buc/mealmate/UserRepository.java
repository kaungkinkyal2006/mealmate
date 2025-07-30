package com.buc.mealmate;

import android.content.Context;
import android.content.SharedPreferences;

public class UserRepository {
    private static final String PREFS_NAME = "MealMateUsers";
    private final SharedPreferences sharedPreferences;

    public UserRepository(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean register(String username, String password) {
        if (sharedPreferences.contains(username)) {
            return false; // user exists
        }
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(username, password);
        editor.apply();
        return true;
    }

    public boolean login(String username, String password) {
        String storedPassword = sharedPreferences.getString(username, null);
        return storedPassword != null && storedPassword.equals(password);
    }
}
