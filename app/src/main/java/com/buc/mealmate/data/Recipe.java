package com.buc.mealmate.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

@Entity(tableName = "recipes")
public class Recipe {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public String ingredients;  // Comma-separated list of ingredients
    public String purchasedIngredients; // Comma-separated purchased items
    public String instructions;

    // New JSON field for per-ingredient locations
    public String purchasedIngredientLocationsJson;

    // Transient map to hold deserialized locations, not stored in DB directly
    private transient Map<String, LatLng> purchasedIngredientLocations;

    private static final Gson gson = new Gson();

    public Recipe(String name, String ingredients, String instructions) {
        this.name = name;
        this.ingredients = ingredients;
        this.instructions = instructions;
        this.purchasedIngredients = ""; // Initially none purchased
        this.purchasedIngredientLocationsJson = "";
    }

    // Deserialize JSON to Map
    public Map<String, LatLng> getPurchasedIngredientLocations() {
        if (purchasedIngredientLocations == null) {
            if (purchasedIngredientLocationsJson == null || purchasedIngredientLocationsJson.isEmpty()) {
                purchasedIngredientLocations = new HashMap<>();
            } else {
                Type type = new TypeToken<Map<String, LatLng>>() {
                }.getType();
                purchasedIngredientLocations = gson.fromJson(purchasedIngredientLocationsJson, type);
                if (purchasedIngredientLocations == null)
                    purchasedIngredientLocations = new HashMap<>();
            }
        }
        return purchasedIngredientLocations;
    }

    // Serialize Map to JSON
    public void setPurchasedIngredientLocations(Map<String, LatLng> map) {
        purchasedIngredientLocations = map;
        purchasedIngredientLocationsJson = gson.toJson(map);
    }

    // Helper class for lat/lng pair
    public static class LatLng {
        public double latitude;
        public double longitude;

        public LatLng(double latitude, double longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }

    public boolean isReadyToCook() {
        if (ingredients == null || ingredients.trim().isEmpty()) return false;

        String[] all = ingredients.split(",");
        String[] purchased = purchasedIngredients == null ? new String[]{} : purchasedIngredients.split(",");

        for (int i = 0; i < all.length; i++) {
            all[i] = all[i].trim();
        }
        for (int i = 0; i < purchased.length; i++) {
            purchased[i] = purchased[i].trim();
        }

        if (purchased.length != all.length) return false;

        for (String ing : all) {
            boolean found = false;
            for (String pur : purchased) {
                if (ing.equalsIgnoreCase(pur)) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }
}
