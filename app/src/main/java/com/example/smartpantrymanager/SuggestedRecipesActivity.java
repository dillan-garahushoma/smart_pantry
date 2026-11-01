package com.example.smartpantrymanager;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartpantrymanager.db.PantryDatabaseHelper;
import com.example.smartpantrymanager.model.PantryItem;
import com.example.smartpantrymanager.model.Recipe;
import com.example.smartpantrymanager.model.RecipeIngredient;
import com.example.smartpantrymanager.service.RecipeMatchingService;
import com.example.smartpantrymanager.service.RecipeMatchingService.RecipeMatchResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SuggestedRecipesActivity extends AppCompatActivity {
    private static final int TAB_CAN_COOK = 0;
    private static final int TAB_ALMOST_THERE = 1;
    private static final int TAB_ALL_RECIPES = 2;

    private PantryDatabaseHelper dbHelper;
    private RecyclerView recipeRecyclerView;
    private View emptyStateContainer;
    private TextView emptyStateText;
    private Button btnEmptyStateAction;
    private EditText recipeSearchInput;

    private TextView tabCanCook;
    private TextView tabAlmostThere;
    private TextView tabAllRecipes;

    private int currentTab = TAB_CAN_COOK;
    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_suggested_recipes);

        dbHelper = new PantryDatabaseHelper(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Recipe Discovery");
        }

        recipeRecyclerView = findViewById(R.id.recipeRecyclerView);
        emptyStateContainer = findViewById(R.id.emptyStateContainer);
        emptyStateText = findViewById(R.id.emptyStateText);
        btnEmptyStateAction = findViewById(R.id.btnEmptyStateAction);
        recipeSearchInput = findViewById(R.id.recipeSearchInput);

        tabCanCook = findViewById(R.id.tabCanCook);
        tabAlmostThere = findViewById(R.id.tabAlmostThere);
        tabAllRecipes = findViewById(R.id.tabAllRecipes);

        recipeRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        tabCanCook.setOnClickListener(v -> switchTab(TAB_CAN_COOK));
        tabAlmostThere.setOnClickListener(v -> switchTab(TAB_ALMOST_THERE));
        tabAllRecipes.setOnClickListener(v -> switchTab(TAB_ALL_RECIPES));

        btnEmptyStateAction.setOnClickListener(v -> {
            if (currentTab != TAB_ALL_RECIPES) {
                switchTab(TAB_ALL_RECIPES);
            } else {
                recipeSearchInput.setText("");
            }
        });

        recipeSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s != null ? s.toString().trim() : "";
                loadRecipes();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        loadRecipes();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadRecipes();
    }

    private void switchTab(int tab) {
        currentTab = tab;
        updateTabStyles();
        loadRecipes();
    }

    private void updateTabStyles() {
        int selectedBg = R.drawable.bg_tab_selected;
        int unselectedBg = R.drawable.bg_tab_unselected;
        int whiteColor = ContextCompat.getColor(this, R.color.white);
        int unselectedColor = ContextCompat.getColor(this, R.color.text_secondary);

        tabCanCook.setBackgroundResource(currentTab == TAB_CAN_COOK ? selectedBg : unselectedBg);
        tabCanCook.setTextColor(currentTab == TAB_CAN_COOK ? whiteColor : unselectedColor);

        tabAlmostThere.setBackgroundResource(currentTab == TAB_ALMOST_THERE ? selectedBg : unselectedBg);
        tabAlmostThere.setTextColor(currentTab == TAB_ALMOST_THERE ? whiteColor : unselectedColor);

        tabAllRecipes.setBackgroundResource(currentTab == TAB_ALL_RECIPES ? selectedBg : unselectedBg);
        tabAllRecipes.setTextColor(currentTab == TAB_ALL_RECIPES ? whiteColor : unselectedColor);
    }

    private void loadRecipes() {
        List<Recipe> allRecipes = dbHelper.getAllRecipes();
        List<PantryItem> pantryItems = dbHelper.getAllPantryItems();
        List<RecipeMatchResult> allMatches = RecipeMatchingService.getAllRecipeMatches(allRecipes, pantryItems);

        int strictCount = 0;
        int almostCount = 0;
        for (RecipeMatchResult match : allMatches) {
            if (match.isStrictMatch()) {
                strictCount++;
            } else if (match.isAlmostThere()) {
                almostCount++;
            }
        }

        // Update tab count labels
        tabCanCook.setText("🍳 Can Cook Now (" + strictCount + ")");
        tabAlmostThere.setText("⏳ Almost There (" + almostCount + ")");
        tabAllRecipes.setText("📖 All Recipes (" + allMatches.size() + ")");

        // Filter based on active tab
        List<RecipeMatchResult> tabResults = new ArrayList<>();
        for (RecipeMatchResult match : allMatches) {
            if (currentTab == TAB_CAN_COOK && match.isStrictMatch()) {
                tabResults.add(match);
            } else if (currentTab == TAB_ALMOST_THERE && match.isAlmostThere()) {
                tabResults.add(match);
            } else if (currentTab == TAB_ALL_RECIPES) {
                tabResults.add(match);
            }
        }

        // Filter based on search query
        List<RecipeMatchResult> displayList = new ArrayList<>();
        if (!currentQuery.isEmpty()) {
            String lowerQuery = currentQuery.toLowerCase(Locale.US);
            for (RecipeMatchResult match : tabResults) {
                boolean nameMatch = match.getRecipe().getName().toLowerCase(Locale.US).contains(lowerQuery);
                boolean ingredientMatch = false;
                for (RecipeIngredient ing : match.getRecipe().getIngredients()) {
                    if (ing.getIngredientName().toLowerCase(Locale.US).contains(lowerQuery)) {
                        ingredientMatch = true;
                        break;
                    }
                }
                if (nameMatch || ingredientMatch) {
                    displayList.add(match);
                }
            }
        } else {
            displayList.addAll(tabResults);
        }

        RecipeAdapter adapter = new RecipeAdapter(displayList);
        recipeRecyclerView.setAdapter(adapter);

        // Manage empty states
        if (displayList.isEmpty()) {
            emptyStateContainer.setVisibility(View.VISIBLE);
            recipeRecyclerView.setVisibility(View.GONE);

            if (currentTab == TAB_CAN_COOK) {
                if (allMatches.isEmpty()) {
                    emptyStateText.setText("No recipes found in the database.");
                    btnEmptyStateAction.setVisibility(View.GONE);
                } else if (almostCount > 0) {
                    emptyStateText.setText("No recipes match 100% of your current pantry yet.\n\nGood news: You are just 1 ingredient away from " + almostCount + " recipes in 'Almost There'!");
                    btnEmptyStateAction.setText("View " + almostCount + " 'Almost There' Recipes");
                    btnEmptyStateAction.setVisibility(View.VISIBLE);
                    btnEmptyStateAction.setOnClickListener(v -> switchTab(TAB_ALMOST_THERE));
                } else {
                    emptyStateText.setText("No recipes match 100% of your current pantry yet.\n\nBrowse all " + allMatches.size() + " recipes to find cooking inspiration and ingredients to add!");
                    btnEmptyStateAction.setText("Browse All " + allMatches.size() + " Recipes");
                    btnEmptyStateAction.setVisibility(View.VISIBLE);
                    btnEmptyStateAction.setOnClickListener(v -> switchTab(TAB_ALL_RECIPES));
                }
            } else if (currentTab == TAB_ALMOST_THERE) {
                emptyStateText.setText("No recipes are missing just 1 ingredient right now.\n\nExplore our complete catalog of " + allMatches.size() + " recipes below!");
                btnEmptyStateAction.setText("Browse All " + allMatches.size() + " Recipes");
                btnEmptyStateAction.setVisibility(View.VISIBLE);
                btnEmptyStateAction.setOnClickListener(v -> switchTab(TAB_ALL_RECIPES));
            } else {
                if (!currentQuery.isEmpty()) {
                    emptyStateText.setText("No recipes found matching \"" + currentQuery + "\".\nTry searching for ingredients like egg, pasta, or rice.");
                    btnEmptyStateAction.setText("Clear Search");
                    btnEmptyStateAction.setVisibility(View.VISIBLE);
                    btnEmptyStateAction.setOnClickListener(v -> recipeSearchInput.setText(""));
                } else {
                    emptyStateText.setText("No recipes available.");
                    btnEmptyStateAction.setVisibility(View.GONE);
                }
            }
        } else {
            emptyStateContainer.setVisibility(View.GONE);
            recipeRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.ViewHolder> {
        private final List<RecipeMatchResult> list;

        public RecipeAdapter(List<RecipeMatchResult> list) {
            this.list = list;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull android.view.ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.item_recipe, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            RecipeMatchResult match = list.get(position);
            Recipe recipe = match.getRecipe();

            holder.title.setText(recipe.getName());
            holder.time.setText(recipe.getPreparationTime());

            // Match Badge styling
            if (match.isStrictMatch()) {
                holder.matchBadge.setText("✓ Ready to Cook");
                holder.matchBadge.setBackgroundResource(R.drawable.bg_status_fresh);
                holder.matchBadge.setTextColor(ContextCompat.getColor(SuggestedRecipesActivity.this, R.color.status_fresh_text));
            } else if (match.isAlmostThere()) {
                RecipeIngredient missing = match.getMissingIngredients().get(0);
                holder.matchBadge.setText("⚠️ Missing: " + missing.getIngredientName());
                holder.matchBadge.setBackgroundResource(R.drawable.bg_status_expiring);
                holder.matchBadge.setTextColor(ContextCompat.getColor(SuggestedRecipesActivity.this, R.color.status_expiring_text));
            } else {
                holder.matchBadge.setText("Pantry: " + match.getMatchedCount() + " of " + match.getTotalRequired() + " ingredients");
                holder.matchBadge.setBackgroundResource(R.drawable.bg_status_fresh);
                holder.matchBadge.setTextColor(ContextCompat.getColor(SuggestedRecipesActivity.this, R.color.teal_primary));
            }

            // Ingredients summary
            StringBuilder details = new StringBuilder();
            for (RecipeIngredient ingredient : recipe.getIngredients()) {
                if (details.length() > 0) {
                    details.append(" • ");
                }
                details.append(ingredient.getQuantity()).append(" ").append(ingredient.getUnit()).append(" ").append(ingredient.getIngredientName());
            }
            holder.summary.setText(details.toString());

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(SuggestedRecipesActivity.this, RecipeDetailActivity.class);
                intent.putExtra("recipe_id", recipe.getId());
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            TextView time;
            TextView matchBadge;
            TextView summary;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.recipeTitle);
                time = itemView.findViewById(R.id.recipeTime);
                matchBadge = itemView.findViewById(R.id.recipeMatchBadge);
                summary = itemView.findViewById(R.id.recipeSummary);
            }
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
