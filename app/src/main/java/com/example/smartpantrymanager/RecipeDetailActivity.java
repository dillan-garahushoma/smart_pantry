package com.example.smartpantrymanager;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.example.smartpantrymanager.db.PantryDatabaseHelper;
import com.example.smartpantrymanager.model.PantryItem;
import com.example.smartpantrymanager.model.Recipe;
import com.example.smartpantrymanager.model.RecipeIngredient;
import com.example.smartpantrymanager.service.RecipeMatchingService;
import com.example.smartpantrymanager.service.RecipeMatchingService.RecipeMatchResult;

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
        RecipeMatchResult matchResult = RecipeMatchingService.evaluateRecipe(recipe, pantryItems);

        TextView titleText = findViewById(R.id.recipeDetailTitle);
        TextView descriptionText = findViewById(R.id.recipeDetailDescription);
        TextView prepTimeText = findViewById(R.id.recipeDetailPrepTime);
        TextView eligibilityText = findViewById(R.id.recipeEligibilityText);
        TextView ingredientText = findViewById(R.id.recipeIngredientList);
        TextView instructionsText = findViewById(R.id.recipeInstructions);

        titleText.setText(recipe.getName());
        descriptionText.setText(recipe.getDescription());
        prepTimeText.setText(recipe.getPreparationTime());

        if (matchResult.isStrictMatch()) {
            eligibilityText.setText("✓ Ready to Cook (All in pantry)");
            eligibilityText.setBackgroundResource(R.drawable.bg_status_fresh);
            eligibilityText.setTextColor(ContextCompat.getColor(this, R.color.status_fresh_text));
        } else if (matchResult.isAlmostThere()) {
            RecipeIngredient missing = matchResult.getMissingIngredients().get(0);
            eligibilityText.setText("⚠️ Missing 1: " + missing.getIngredientName());
            eligibilityText.setBackgroundResource(R.drawable.bg_status_expiring);
            eligibilityText.setTextColor(ContextCompat.getColor(this, R.color.status_expiring_text));
        } else {
            eligibilityText.setText("Missing " + matchResult.getMissingIngredients().size() + " Ingredients");
            eligibilityText.setBackgroundResource(R.drawable.bg_status_expiring);
            eligibilityText.setTextColor(ContextCompat.getColor(this, R.color.status_expiring_text));
        }

        // Format checklist of ingredients
        StringBuilder ingredients = new StringBuilder();
        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            boolean hasIt = false;
            for (RecipeIngredient satisfied : matchResult.getSatisfiedIngredients()) {
                if (satisfied.getIngredientName().equalsIgnoreCase(ingredient.getIngredientName())) {
                    hasIt = true;
                    break;
                }
            }

            if (hasIt) {
                ingredients.append("✓  ")
                        .append(ingredient.getQuantity()).append(" ").append(ingredient.getUnit())
                        .append(" ").append(ingredient.getIngredientName())
                        .append("  (in pantry)\n\n");
            } else {
                ingredients.append("○  ")
                        .append(ingredient.getQuantity()).append(" ").append(ingredient.getUnit())
                        .append(" ").append(ingredient.getIngredientName())
                        .append("  (need to get)\n\n");
            }
        }
        ingredientText.setText(ingredients.toString().trim());

        // Format numbered steps
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
