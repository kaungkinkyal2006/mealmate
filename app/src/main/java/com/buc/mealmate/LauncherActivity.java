package com.buc.mealmate;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class LauncherActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "MealMatePrefs";
    private static final String KEY_LOGGED_IN_USER = "loggedInUser";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);

        ImageView logo = findViewById(R.id.logoImage);
        TextView appName = findViewById(R.id.appName);

        // Scale animation (zoom in)
        ScaleAnimation scaleAnim = new ScaleAnimation(
                0.8f, 1f, 0.8f, 1f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
                ScaleAnimation.RELATIVE_TO_SELF, 0.5f);
        scaleAnim.setDuration(800);

        // Fade-in animation
        AlphaAnimation fadeAnim = new AlphaAnimation(0, 1);
        fadeAnim.setDuration(800);

        // Start animations
        logo.startAnimation(scaleAnim);
        logo.startAnimation(fadeAnim);
        appName.startAnimation(fadeAnim);

        // Modern Status Bar styling
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getResources().getColor(R.color.primaryColor));
        window.setNavigationBarColor(getResources().getColor(R.color.primaryColor));

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

        // Delay to move to next screen
        new Handler().postDelayed(() -> {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String loggedInUser = prefs.getString(KEY_LOGGED_IN_USER, null);

            if (loggedInUser != null) {
                startActivity(new Intent(LauncherActivity.this, RecipeListActivity.class));
            } else {
                startActivity(new Intent(LauncherActivity.this, MainActivityPage.class));
            }
            finish();
        }, 1500);  // Splash delay
    }
}
