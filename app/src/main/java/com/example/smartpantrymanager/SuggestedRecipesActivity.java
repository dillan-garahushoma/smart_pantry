package com.example.smartpantrymanager;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartpantrymanager.db.PantryDatabaseHelper;
import com.example.smartpantrymanager.model.PantryItem;
import com.example.smartpantrymanager.model.Recipe;
import com.example.smartpantrymanager.model.RecipeIngredient;
import com.example.smartpantrymanager.service.RecipeMatchingService;

import java.util.List;

public class SuggestedRecipesActivity extends AppCompatActivity {
    private PantryDatabaseHelper dbHelper;
    private RecyclerView recipeRecyclerView;
    private TextView emptyStateText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggested_recipes);

        dbHelper = new PantryDatabaseHelper(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Suggested Recipes");
        }

        recipeRecyclerView = findViewById(R.id.recipeRecyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);
        recipeRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        loadRecipes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecipes();
    }

    private void loadRecipes() {
        List<Recipe> allRecipes = dbHelper.getAllRecipes();
        List<PantryItem> pantryItems = dbHelper.getAllPantryItems();
        List<Recipe> eligible = RecipeMatchingService.getEligibleRecipes(allRecipes, pantryItems);

        RecyclerView.Adapter<RecyclerView.ViewHolder> adapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
                View view = getLayoutInflater().inflate(R.layout.item_recipe, parent, false);
                return new RecyclerView.ViewHolder(view) {
                };
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                Recipe recipe = eligible.get(position);
                TextView title = holder.itemView.findViewById(R.id.recipeTitle);
                TextView time = holder.itemView.findViewById(R.id.recipeTime);
                TextView summary = holder.itemView.findViewById(R.id.recipeSummary);
                title.setText(recipe.getName());
                time.setText(recipe.getPreparationTime());

                StringBuilder details = new StringBuilder();
                for (RecipeIngredient ingredient : recipe.getIngredients()) {
                    if (details.length() > 0) {
                        details.append(" • ");
                    }
                    details.append(ingredient.getQuantity()).append(" ").append(ingredient.getUnit()).append(" ").append(ingredient.getIngredientName());
                }
                summary.setText(details.toString());

                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(SuggestedRecipesActivity.this, RecipeDetailActivity.class);
                    intent.putExtra("recipe_id", recipe.getId());
                    startActivity(intent);
                });
            }

            @Override
            public int getItemCount() {
                return eligible.size();
            }
        };

        recipeRecyclerView.setAdapter(adapter);

        if (eligible.isEmpty()) {
            emptyStateText.setVisibility(View.VISIBLE);
            recipeRecyclerView.setVisibility(View.GONE);
        } else {
            emptyStateText.setVisibility(View.GONE);
            recipeRecyclerView.setVisibility(View.VISIBLE);
        }
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
