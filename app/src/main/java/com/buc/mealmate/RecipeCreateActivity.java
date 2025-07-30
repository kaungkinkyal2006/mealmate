package com.buc.mealmate;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.buc.mealmate.data.AppDatabase;
import com.buc.mealmate.data.Recipe;
import com.buc.mealmate.databinding.ActivityRecipeCreateBinding;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

public class RecipeCreateActivity extends AppCompatActivity {

    private ActivityRecipeCreateBinding binding;
    private AppDatabase db;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityRecipeCreateBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = AppDatabase.getInstance(this);

        setupSystemBars();

        // Add ingredient chip
        binding.btnAddIngredient.setOnClickListener(v -> {
            String ingredient = binding.ingredientInput.getText().toString().trim();
            if (TextUtils.isEmpty(ingredient)) {
                Toast.makeText(this, "Enter an ingredient", Toast.LENGTH_SHORT).show();
                return;
            }

            addIngredientChip(ingredient);
            binding.ingredientInput.setText("");
        });

        // Save recipe
        binding.saveRecipeButton.setOnClickListener(v -> saveRecipe());
    }

    private void setupSystemBars() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getResources().getColor(R.color.white));
        window.setNavigationBarColor(getResources().getColor(R.color.white));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        } else {
            View decor = window.getDecorView();
            decor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }

    private void addIngredientChip(String ingredient) {
        ChipGroup chipGroup = binding.chipGroupIngredients;

        // Prevent duplicates
        for (int i = 0; i < chipGroup.getChildCount(); i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            if (chip.getText().toString().equalsIgnoreCase(ingredient)) {
                Toast.makeText(this, "Ingredient already added", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        Chip chip = new Chip(this);
        chip.setText(ingredient);
        chip.setCloseIconVisible(true);
        chip.setChipBackgroundColorResource(R.color.primaryColor);
        chip.setCloseIconTintResource(R.color.primaryColor);
        chip.setTextColor(getResources().getColor(R.color.black));
        chip.setOnCloseIconClickListener(v -> chipGroup.removeView(chip));

        chipGroup.addView(chip);
    }


    private void saveRecipe() {
        String name = binding.recipeNameInput.getText().toString().trim();
        String instructions = binding.instructionsInput.getText().toString().trim();
        ChipGroup chipGroup = binding.chipGroupIngredients;
        int chipCount = chipGroup.getChildCount();

        if (name.isEmpty()) {
            Toast.makeText(this, "Enter recipe name", Toast.LENGTH_SHORT).show();
            return;
        }

        if (chipCount == 0) {
            Toast.makeText(this, "Add at least one ingredient", Toast.LENGTH_SHORT).show();
            return;
        }

        if (instructions.isEmpty()) {
            Toast.makeText(this, "Enter preparation instructions", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder ingredientsJoined = new StringBuilder();
        for (int i = 0; i < chipCount; i++) {
            Chip chip = (Chip) chipGroup.getChildAt(i);
            ingredientsJoined.append(chip.getText());
            if (i < chipCount - 1) ingredientsJoined.append(",");
        }

        Recipe recipe = new Recipe(name, ingredientsJoined.toString(), instructions);

        new Thread(() -> {
            db.recipeDao().insert(recipe);
            runOnUiThread(() -> {
                Toast.makeText(this, "Recipe saved", Toast.LENGTH_SHORT).show();
                finish();
            });
        }).start();
    }
}
