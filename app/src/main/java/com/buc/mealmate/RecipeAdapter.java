package com.buc.mealmate;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.buc.mealmate.data.Recipe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.ViewHolder> {

    private final Context context;
    private List<Recipe> recipes;
    private final OnRecipeDeleteListener deleteListener;

    private final Set<Integer> selectedPositions = new HashSet<>();
    private boolean selectionMode = false;

    public interface OnRecipeDeleteListener {
        void onDeleteClicked(Recipe recipe);
    }

    public RecipeAdapter(Context context, List<Recipe> recipes, OnRecipeDeleteListener deleteListener) {
        this.context = context;
        this.recipes = recipes != null ? recipes : new ArrayList<>();
        this.deleteListener = deleteListener;
    }

    public void setRecipes(List<Recipe> recipes) {
        this.recipes = recipes != null ? recipes : new ArrayList<>();
        clearSelection();
        notifyDataSetChanged();
    }

    public void setSelectionMode(boolean enabled) {
        this.selectionMode = enabled;
        if (!enabled) clearSelection();
        notifyDataSetChanged();
    }

    public void clearSelection() {
        selectedPositions.clear();
        notifyDataSetChanged();
    }

    public List<Recipe> getSelectedRecipes() {
        List<Recipe> selected = new ArrayList<>();
        for (Integer pos : selectedPositions) {
            if (pos >= 0 && pos < recipes.size()) selected.add(recipes.get(pos));
        }
        return selected;
    }

    @NonNull
    @Override
    public RecipeAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recipe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeAdapter.ViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        holder.tvName.setText(recipe.name);
        holder.tvStatus.setText(recipe.isReadyToCook() ? "Ready" : "Not Ready");

        holder.checkBox.setOnCheckedChangeListener(null); // clear listener to avoid recycling issues
        holder.checkBox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
        holder.checkBox.setChecked(selectedPositions.contains(position));
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) selectedPositions.add(position);
            else selectedPositions.remove(position);
        });

        holder.card.setOnClickListener(v -> {
            if (selectionMode) {
                holder.checkBox.setChecked(!holder.checkBox.isChecked());
            } else {
                Intent intent = new Intent(context, RecipeDetailActivity.class);
                intent.putExtra("recipe_id", recipe.id);
                context.startActivity(intent);
            }
        });

        holder.btnDelete.setVisibility(selectionMode ? View.GONE : View.VISIBLE);
        holder.btnDelete.setOnClickListener(v -> {
            if (deleteListener != null) deleteListener.onDeleteClicked(recipe);
        });
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CardView card;
        TextView tvName, tvStatus;
        ImageButton btnDelete;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.cardRecipe);
            tvName = itemView.findViewById(R.id.tvRecipeName);
            tvStatus = itemView.findViewById(R.id.tvRecipeStatus);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            checkBox = itemView.findViewById(R.id.checkboxSelect);
        }
    }
}
