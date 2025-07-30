package com.buc.mealmate;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.buc.mealmate.databinding.ActivityMainPageBinding;

public class MainActivityPage extends AppCompatActivity {
    private ActivityMainPageBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainPageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Status bar color (light background, dark icons)
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getResources().getColor(R.color.white));
        window.setNavigationBarColor(getResources().getColor(R.color.white));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = window.getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(0, WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS);
            }
        } else {
            View decor = window.getDecorView();
            decor.setSystemUiVisibility(0);
        }

        binding.btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(MainActivityPage.this, LoginActivity.class));
        });

        binding.btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(MainActivityPage.this, RegisterActivity.class));
        });
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
        finishAffinity();
    }
}
