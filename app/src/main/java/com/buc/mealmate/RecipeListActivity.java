package com.buc.mealmate;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.buc.mealmate.data.AppDatabase;
import com.buc.mealmate.data.Recipe;

import java.util.ArrayList;
import java.util.List;

public class RecipeListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecipeAdapter adapter;
    private AppDatabase db;
    private View btnAddRecipe, btnSendSms, btnCancelSelection;
    private Toolbar toolbar;

    private static final String PREFS_NAME = "MealMatePrefs";
    private static final String KEY_LOGGED_IN_USER = "loggedInUser";

    private boolean isInSelectionMode = false;
    private List<Recipe> pendingSmsRecipes;
    private String pendingSmsPhoneNumber;

    private static final int SMS_PERMISSION_REQUEST_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_list_page);

        db = AppDatabase.getInstance(this);

        setupStatusBar();
        setupToolbar();
        setupViews();
        setupListeners();
    }

    private void setupStatusBar() {
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getColor(R.color.white));
        window.setNavigationBarColor(getColor(R.color.white));

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

    private void setupToolbar() {
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String username = prefs.getString(KEY_LOGGED_IN_USER, "User");
        toolbar.setSubtitle(getString(R.string.welcome_user, username));
    }

    private void setupViews() {
        recyclerView = findViewById(R.id.recyclerViewRecipes);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        btnAddRecipe = findViewById(R.id.btnAddRecipe);
        btnSendSms = findViewById(R.id.btnSendSms);
        btnCancelSelection = findViewById(R.id.btnCancelSelection);
        btnCancelSelection.setVisibility(View.GONE);

        btnSendSms.setEnabled(false);
        btnSendSms.setTag("Select Recipes");

        loadRecipes();
    }

    private void setupListeners() {
        btnAddRecipe.setOnClickListener(v -> startActivity(new Intent(this, RecipeCreateActivity.class)));

        btnSendSms.setOnClickListener(v -> {
            if (adapter == null) {
                Toast.makeText(this, "No recipes loaded", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!isInSelectionMode) {
                enterSelectionMode();
            } else {
                List<Recipe> selectedRecipes = adapter.getSelectedRecipes();
                if (selectedRecipes.isEmpty()) {
                    Toast.makeText(this, "Please select at least one recipe.", Toast.LENGTH_SHORT).show();
                    return;
                }
                promptPhoneNumberAndSendSms(selectedRecipes);
            }
        });

        btnCancelSelection.setOnClickListener(v -> {
            exitSelectionMode();
            Toast.makeText(this, "Selection cancelled", Toast.LENGTH_SHORT).show();
        });
    }

    private void enterSelectionMode() {
        isInSelectionMode = true;
        adapter.setSelectionMode(true);
        updateSendButtonText("Send SMS");
        btnCancelSelection.setVisibility(View.VISIBLE);
        Toast.makeText(this, "Select recipes to send via SMS", Toast.LENGTH_SHORT).show();
    }

    private void exitSelectionMode() {
        isInSelectionMode = false;
        if (adapter != null) {
            adapter.setSelectionMode(false);
            adapter.clearSelection();
        }
        updateSendButtonText("Select Recipes");
        btnCancelSelection.setVisibility(View.GONE);
        pendingSmsPhoneNumber = null;
        pendingSmsRecipes = null;
    }

    private void updateSendButtonText(String text) {
        if (btnSendSms instanceof TextView) {
            ((TextView) btnSendSms).setText(text);
        }
    }

    private void promptPhoneNumberAndSendSms(List<Recipe> selectedRecipes) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Enter phone number");
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        builder.setView(input);

        builder.setPositiveButton("Send", (dialog, which) -> {
            String phoneNumber = input.getText().toString().trim();
            if (phoneNumber.isEmpty()) {
                Toast.makeText(this, "Phone number cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS},
                        SMS_PERMISSION_REQUEST_CODE);
                pendingSmsPhoneNumber = phoneNumber;
                pendingSmsRecipes = selectedRecipes;
            } else {
                sendSmsToSelected(phoneNumber, selectedRecipes);
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void sendSmsToSelected(String phoneNumber, List<Recipe> selectedRecipes) {
        SmsManager smsManager = SmsManager.getDefault();
        for (Recipe recipe : selectedRecipes) {
            StringBuilder message = new StringBuilder();
            message.append("Recipe: ").append(recipe.name).append("\n");
            if (recipe.ingredients != null && !recipe.ingredients.isEmpty()) {
                String formattedIngredients = recipe.ingredients.replaceAll("\\s*,\\s*", ", ");
                message.append("Ingredients: ").append(formattedIngredients).append("\n");
            }
            if (recipe.instructions != null && !recipe.instructions.isEmpty()) {
                message.append("Instructions: ").append(recipe.instructions);
            }
            List<String> parts = smsManager.divideMessage(message.toString());
            smsManager.sendMultipartTextMessage(phoneNumber, null, new ArrayList<>(parts), null, null);
        }
        exitSelectionMode();
        Toast.makeText(this, "SMS sent to selected recipes", Toast.LENGTH_SHORT).show();
    }

    private void loadRecipes() {
        List<Recipe> recipes = db.recipeDao().getAll();
        if (adapter == null) {
            adapter = new RecipeAdapter(this, recipes, recipe -> confirmDelete(recipe));
            recyclerView.setAdapter(adapter);
        } else {
            adapter.setRecipes(recipes);
        }
        btnSendSms.setEnabled(!recipes.isEmpty());
    }

    private void confirmDelete(Recipe recipe) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Recipe")
                .setMessage("Are you sure you want to delete \"" + recipe.name + "\"?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    db.recipeDao().delete(recipe);
                    Toast.makeText(this, "Recipe deleted", Toast.LENGTH_SHORT).show();
                    loadRecipes();
                })
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (pendingSmsPhoneNumber != null && pendingSmsRecipes != null) {
                    sendSmsToSelected(pendingSmsPhoneNumber, pendingSmsRecipes);
                }
            } else {
                Toast.makeText(this, "SMS Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecipes();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.drawer_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            prefs.edit().remove(KEY_LOGGED_IN_USER).apply();
            Intent intent = new Intent(this, MainActivityPage.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
