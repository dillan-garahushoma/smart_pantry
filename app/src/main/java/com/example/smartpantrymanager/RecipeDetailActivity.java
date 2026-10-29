package com.example.smartpantrymanager;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.smartpantrymanager.db.PantryDatabaseHelper;
import com.example.smartpantrymanager.model.PantryItem;
import com.example.smartpantrymanager.model.Recipe;
import com.example.smartpantrymanager.model.RecipeIngredient;
import com.example.smartpantrymanager.service.RecipeMatchingService;

import java.util.List;

public class RecipeDetailActivity extends AppCompatActivity {
    private PantryDatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        dbHelper = new PantryDatabaseHelper(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Recipe Details");
        }

        long recipeId = getIntent().getLongExtra("recipe_id", -1L);
        Recipe recipe = dbHelper.getRecipeById(recipeId);
        if (recipe == null) {
            finish();
            return;
        }

        List<PantryItem> pantryItems = dbHelper.getAllPantryItems();
        boolean eligible = RecipeMatchingService.isRecipeEligible(recipe, pantryItems);

        TextView titleText = findViewById(R.id.recipeDetailTitle);
        TextView descriptionText = findViewById(R.id.recipeDetailDescription);
        TextView prepTimeText = findViewById(R.id.recipeDetailPrepTime);
        TextView eligibilityText = findViewById(R.id.recipeEligibilityText);
        TextView ingredientText = findViewById(R.id.recipeIngredientList);
        TextView instructionsText = findViewById(R.id.recipeInstructions);

        titleText.setText(recipe.getName());
        descriptionText.setText(recipe.getDescription());
        prepTimeText.setText("Prep time: " + recipe.getPreparationTime());
        eligibilityText.setText(eligible ? "Eligible with your pantry" : "Not eligible with your current pantry");

        StringBuilder ingredients = new StringBuilder();
        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            ingredients.append("• ").append(ingredient.getQuantity()).append(" ").append(ingredient.getUnit()).append(" ").append(ingredient.getIngredientName()).append("\n");
        }
        ingredientText.setText(ingredients.toString());
        instructionsText.setText(recipe.getInstructions());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
