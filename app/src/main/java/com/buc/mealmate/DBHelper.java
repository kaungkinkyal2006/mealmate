// MealMate/app/src/main/java/com/buc/mealmate/DBHelper.java
package com.buc.mealmate;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class DBHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "mealmate.db";
    private static final int DATABASE_VERSION = 1;

    // Users table
    private static final String TABLE_USERS = "users";
    private static final String COL_USER_ID = "id";
    private static final String COL_USERNAME = "username";
    private static final String COL_PASSWORD = "password";

    // Recipes table
    private static final String TABLE_RECIPES = "recipes";
    private static final String COL_RECIPE_ID = "id";
    private static final String COL_TITLE = "title";
    private static final String COL_INGREDIENTS = "ingredients";
    private static final String COL_INSTRUCTIONS = "instructions";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createUsers = "CREATE TABLE " + TABLE_USERS + "("
                + COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_USERNAME + " TEXT UNIQUE,"
                + COL_PASSWORD + " TEXT"
                + ")";
        db.execSQL(createUsers);

        String createRecipes = "CREATE TABLE " + TABLE_RECIPES + "("
                + COL_RECIPE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COL_TITLE + " TEXT,"
                + COL_INGREDIENTS + " TEXT,"
                + COL_INSTRUCTIONS + " TEXT"
                + ")";
        db.execSQL(createRecipes);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Upgrade logic if needed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPES);
        onCreate(db);
    }

    // Insert new user
    public boolean insertUser(String username, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_USERNAME, username);
        cv.put(COL_PASSWORD, password);

        long result = db.insert(TABLE_USERS, null, cv);
        return result != -1;
    }

    // Check if username exists
    public boolean checkUsernameExists(String username) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COL_USER_ID},
                COL_USERNAME + "=?",
                new String[]{username},
                null, null, null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    // Check username and password
    public boolean checkUser(String username, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_USERS,
                new String[]{COL_USER_ID},
                COL_USERNAME + "=? AND " + COL_PASSWORD + "=?",
                new String[]{username, password},
                null, null, null);
        boolean valid = cursor.getCount() > 0;
        cursor.close();
        return valid;
    }

    // Insert recipe
    public boolean insertRecipe(String title, String ingredients, String instructions) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TITLE, title);
        cv.put(COL_INGREDIENTS, ingredients);
        cv.put(COL_INSTRUCTIONS, instructions);

        long result = db.insert(TABLE_RECIPES, null, cv);
        return result != -1;
    }

    // Get all recipes as formatted strings
    public List<String> getAllRecipesText() {
        List<String> recipesList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_RECIPES,
                new String[]{COL_TITLE, COL_INGREDIENTS, COL_INSTRUCTIONS},
                null, null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                String title = cursor.getString(0);
                String ingredients = cursor.getString(1);
                String instructions = cursor.getString(2);
                String recipeStr = "Recipe: " + title + "\nIngredients: " + ingredients + "\nInstructions: " + instructions;
                recipesList.add(recipeStr);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return recipesList;
    }
}
