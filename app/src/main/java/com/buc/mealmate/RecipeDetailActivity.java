package com.buc.mealmate;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.buc.mealmate.data.AppDatabase;
import com.buc.mealmate.data.Recipe;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecipeDetailActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST = 1001;

    private MaterialToolbar topAppBar;
    private RecyclerView rvIngredients;
    private Recipe recipe;
    private AppDatabase db;
    private IngredientsAdapter ingredientsAdapter;
    private MaterialButton btnSave;

    private FusedLocationProviderClient fusedLocationClient;

    // Map to hold saved ingredient purchase locations (loaded from recipe)
    private Map<String, Recipe.LatLng> savedLocations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        topAppBar = findViewById(R.id.topAppBar);
        rvIngredients = findViewById(R.id.rvIngredients);
        btnSave = findViewById(R.id.btnSave);

        db = AppDatabase.getInstance(this);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        int recipeId = getIntent().getIntExtra("recipe_id", -1);
        if (recipeId == -1) {
            Toast.makeText(this, "Invalid recipe", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        recipe = db.recipeDao().getById(recipeId);
        if (recipe == null) {
            Toast.makeText(this, "Recipe not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        topAppBar.setTitle(recipe.name);
        topAppBar.setNavigationOnClickListener(v -> onBackPressed());

        savedLocations = recipe.getPurchasedIngredientLocations();

        rvIngredients.setLayoutManager(new LinearLayoutManager(this));
        ingredientsAdapter = new IngredientsAdapter(parseIngredients(), parsePurchasedIngredients(), savedLocations);
        rvIngredients.setAdapter(ingredientsAdapter);

        findViewById(R.id.tvInstructions).setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.tvInstructions)).setText(recipe.instructions);

        btnSave.setOnClickListener(v -> {
            updatePurchasedIngredients();

            // Save purchase locations map to recipe entity
            recipe.setPurchasedIngredientLocations(ingredientsAdapter.getPurchaseLocationsAsLatLngMap());

            new Thread(() -> {
                db.recipeDao().update(recipe);
                runOnUiThread(() -> {
                    Toast.makeText(this, recipe.isReadyToCook() ?
                            "All ingredients purchased! Ready to cook." :
                            "Progress saved.", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }).start();
        });
    }

    private List<String> parseIngredients() {
        if (recipe.ingredients == null || recipe.ingredients.trim().isEmpty())
            return new ArrayList<>();
        String[] split = recipe.ingredients.split(",");
        List<String> list = new ArrayList<>();
        for (String s : split) list.add(s.trim());
        return list;
    }

    private List<String> parsePurchasedIngredients() {
        if (recipe.purchasedIngredients == null || recipe.purchasedIngredients.trim().isEmpty())
            return new ArrayList<>();
        String[] split = recipe.purchasedIngredients.split(",");
        List<String> list = new ArrayList<>();
        for (String s : split) list.add(s.trim());
        return list;
    }

    private void updatePurchasedIngredients() {
        List<String> purchasedNow = ingredientsAdapter.getPurchasedIngredients();
        recipe.purchasedIngredients = String.join(",", purchasedNow);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                ingredientsAdapter.retryPendingLocationFetch();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class IngredientsAdapter extends RecyclerView.Adapter<IngredientsAdapter.ViewHolder> {

        private final List<String> ingredients;
        private final List<String> purchased;
        private final List<Boolean> purchasedStates;

        // Map ingredient -> location string ("lat, lon")
        private final Map<String, String> purchaseLocations;

        // Track which ingredient position is currently waiting for location permission
        private int pendingLocationPos = -1;

        IngredientsAdapter(List<String> ingredients, List<String> purchased, Map<String, Recipe.LatLng> savedLocations) {
            this.ingredients = ingredients;
            this.purchased = purchased;
            this.purchasedStates = new ArrayList<>();
            for (String ing : ingredients) {
                purchasedStates.add(purchased.contains(ing));
            }

            this.purchaseLocations = new HashMap<>();
            if (savedLocations != null) {
                for (Map.Entry<String, Recipe.LatLng> entry : savedLocations.entrySet()) {
                    Recipe.LatLng latLng = entry.getValue();
                    String latLonStr = latLng.latitude + ", " + latLng.longitude;
                    this.purchaseLocations.put(entry.getKey(), latLonStr);
                }
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View v = getLayoutInflater().inflate(R.layout.item_ingredient, parent, false);
            return new ViewHolder(v);
        }


        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String ingredient = ingredients.get(position);
            Context context = holder.itemView.getContext();

            holder.tvIngredientName.setText(ingredient);

            // Remove previous listener before changing checked state to avoid unwanted triggers
            holder.cbPurchasedToggle.setOnCheckedChangeListener(null);
            boolean isPurchased = purchasedStates.get(position);
            holder.cbPurchasedToggle.setChecked(isPurchased);

            if (isPurchased && purchaseLocations.containsKey(ingredient)) {
                String latLon = purchaseLocations.get(ingredient);

                // Only toast for debugging if really needed, otherwise remove this line
                // Toast.makeText(context, "pls check again " + latLon, Toast.LENGTH_SHORT).show();

                holder.tvPurchasedLabel.setVisibility(View.VISIBLE);
                holder.tvPurchasedLabel.setText("Bought from here: " + latLon);

                holder.ivCopy.setVisibility(View.VISIBLE);
                holder.ivCopy.setOnClickListener(v -> {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                            context.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        android.content.ClipData clip = android.content.ClipData.newPlainText("LatLong", latLon);
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(context, "Location copied: " + latLon, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(context, "Failed to access clipboard", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                holder.tvPurchasedLabel.setVisibility(View.GONE);
                holder.tvPurchasedLabel.setText("");

                holder.ivCopy.setVisibility(View.GONE);
                holder.ivCopy.setOnClickListener(null);
            }

            // Re-attach the listener after setting checked state
            holder.cbPurchasedToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                purchasedStates.set(position, isChecked);

                if (isChecked) {
                    fetchLocationForIngredient(position, ingredient, holder);
                } else {
                    purchaseLocations.remove(ingredient);
                    holder.tvPurchasedLabel.setVisibility(View.GONE);
                    holder.tvPurchasedLabel.setText("");
                    holder.ivCopy.setVisibility(View.GONE);
                    holder.ivCopy.setOnClickListener(null);
                }
            });
        }


        @Override
        public int getItemCount() {
            return ingredients.size();
        }

        List<String> getPurchasedIngredients() {
            List<String> list = new ArrayList<>();
            for (int i = 0; i < ingredients.size(); i++) {
                if (purchasedStates.get(i)) {
                    list.add(ingredients.get(i));
                }
            }
            return list;
        }

        // Convert purchaseLocations map of String lat/lon into Recipe.LatLng map for saving
        Map<String, Recipe.LatLng> getPurchaseLocationsAsLatLngMap() {
            Map<String, Recipe.LatLng> map = new HashMap<>();
            for (Map.Entry<String, String> entry : purchaseLocations.entrySet()) {
                String[] parts = entry.getValue().split(",");
                if (parts.length == 2) {
                    try {
                        double lat = Double.parseDouble(parts[0].trim());
                        double lon = Double.parseDouble(parts[1].trim());
                        map.put(entry.getKey(), new Recipe.LatLng(lat, lon));
                    } catch (NumberFormatException e) {
                        // ignore invalid format
                    }
                }
            }
            return map;
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvIngredientName;
            CheckBox cbPurchasedToggle;
            TextView tvPurchasedLabel;

            ImageView ivCopy;

            ViewHolder(@NonNull android.view.View itemView) {
                super(itemView);
                tvIngredientName = itemView.findViewById(R.id.tvIngredientName);
                cbPurchasedToggle = itemView.findViewById(R.id.cbPurchasedToggle);
                tvPurchasedLabel = itemView.findViewById(R.id.tvPurchasedLabel);
                ivCopy = itemView.findViewById(R.id.ivCopy);
            }
        }

        private void fetchLocationForIngredient(int position, String ingredient, ViewHolder holder) {
            if (ActivityCompat.checkSelfPermission(RecipeDetailActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                pendingLocationPos = position;
                ActivityCompat.requestPermissions(RecipeDetailActivity.this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        LOCATION_PERMISSION_REQUEST);
                return;
            }

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(RecipeDetailActivity.this, location -> {
                        if (location != null) {
                            String latLon = location.getLatitude() + ", " + location.getLongitude();
                            purchaseLocations.put(ingredient, latLon);
                            holder.tvPurchasedLabel.setVisibility(View.VISIBLE);
                            holder.tvPurchasedLabel.setText("Bought from here: " + latLon);
                        } else {
                            Toast.makeText(RecipeDetailActivity.this,
                                    "Unable to fetch location for " + ingredient, Toast.LENGTH_SHORT).show();
                            purchasedStates.set(position, false);
                            notifyItemChanged(position);
                        }
                    });
        }

        void retryPendingLocationFetch() {
            if (pendingLocationPos != -1) {
                notifyItemChanged(pendingLocationPos);
                pendingLocationPos = -1;
            }
        }
    }
}
