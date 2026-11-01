package com.example.smartpantrymanager;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.smartpantrymanager.adapter.PantryAdapter;
import com.example.smartpantrymanager.db.PantryDatabaseHelper;
import com.example.smartpantrymanager.model.PantryItem;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private PantryDatabaseHelper dbHelper;
    private PantryAdapter pantryAdapter;
    private RecyclerView pantryRecyclerView;
    private View emptyPantryContainer;
    private TextView emptyPantryView;
    private TextView totalItemsCountText;
    private TextView expiringCountText;
    private EditText searchInput;
    private Spinner sortSpinner;
    private BottomNavigationView bottomNavigationView;

    private String currentSort = "name";

    private Button btnViewSuggestedRecipes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new PantryDatabaseHelper(this);
        dbHelper.seedRecipesIfNeeded();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        pantryRecyclerView = findViewById(R.id.pantryRecyclerView);
        emptyPantryContainer = findViewById(R.id.emptyPantryContainer);
        emptyPantryView = findViewById(R.id.emptyPantryView);
        totalItemsCountText = findViewById(R.id.totalItemsCountText);
        expiringCountText = findViewById(R.id.expiringCountText);
        searchInput = findViewById(R.id.searchInput);
        sortSpinner = findViewById(R.id.sortSpinner);
        btnViewSuggestedRecipes = findViewById(R.id.btnViewSuggestedRecipes);
        FloatingActionButton fabAddIngredient = findViewById(R.id.fabAddIngredient);
        bottomNavigationView = findViewById(R.id.bottomNavigationView);

        pantryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        pantryAdapter = new PantryAdapter(new PantryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(PantryItem item) {
                Intent intent = new Intent(MainActivity.this, AddEditIngredientActivity.class);
                intent.putExtra("ingredient_id", item.getId());
                startActivity(intent);
            }

            @Override
            public void onDeleteClick(PantryItem item) {
                confirmDeleteItem(item);
            }
        });
        pantryRecyclerView.setAdapter(pantryAdapter);

        setupSortSpinner();
        setupSearch();

        fabAddIngredient.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditIngredientActivity.class);
            startActivity(intent);
        });

        btnViewSuggestedRecipes.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SuggestedRecipesActivity.class);
            startActivity(intent);
        });

        setupBottomNavigation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigationView != null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_pantry);
        }
        loadPantryData();
    }

    private void setupSortSpinner() {
        String[] sortOptions = {"Sort by: Name (A-Z)", "Sort by: Expiry Date", "Sort by: Quantity"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sortOptions);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sortSpinner.setAdapter(spinnerAdapter);

        sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 1:
                        currentSort = "expiry";
                        break;
                    case 2:
                        currentSort = "quantity";
                        break;
                    case 0:
                    default:
                        currentSort = "name";
                        break;
                }
                loadPantryData();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                loadPantryData();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_pantry) {
                return true;
            } else if (itemId == R.id.nav_recipes) {
                startActivity(new Intent(MainActivity.this, SuggestedRecipesActivity.class));
                return true;
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;
            }
            return false;
        });
    }

    private void loadPantryData() {
        String query = searchInput.getText() != null ? searchInput.getText().toString().trim() : "";
        List<PantryItem> items = dbHelper.getPantryItems(query, currentSort);
        pantryAdapter.setItems(items);

        // Update counts
        List<PantryItem> allItems = dbHelper.getAllPantryItems();
        totalItemsCountText.setText(allItems.size() + (allItems.size() == 1 ? " Item" : " Items"));

        // Check ready-to-cook recipes for hero button
        if (btnViewSuggestedRecipes != null) {
            List<com.example.smartpantrymanager.model.Recipe> eligible =
                    com.example.smartpantrymanager.service.RecipeMatchingService.getEligibleRecipes(dbHelper.getAllRecipes(), allItems);
            if (!eligible.isEmpty()) {
                btnViewSuggestedRecipes.setText("✨ " + eligible.size() + (eligible.size() == 1 ? " Recipe" : " Recipes") + " Ready to Cook!");
            } else {
                btnViewSuggestedRecipes.setText("✨ Explore Recipes & Suggestions");
            }
        }

        int expiringCount = 0;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        Date now = new Date();
        for (PantryItem item : allItems) {
            String exp = item.getExpiryDate();
            if (exp != null && !exp.trim().isEmpty()) {
                try {
                    Date expDate = sdf.parse(exp);
                    if (expDate != null) {
                        long diff = expDate.getTime() - now.getTime();
                        long days = diff / (1000 * 60 * 60 * 24);
                        if (days <= 3) {
                            expiringCount++;
                        }
                    }
                } catch (ParseException ignored) {
                }
            }
        }
        expiringCountText.setText(expiringCount + " Expiring");

        if (items.isEmpty()) {
            if (emptyPantryContainer != null) {
                emptyPantryContainer.setVisibility(View.VISIBLE);
            }
            emptyPantryView.setVisibility(View.VISIBLE);
            pantryRecyclerView.setVisibility(View.GONE);
            if (!query.isEmpty()) {
                emptyPantryView.setText("No ingredients matching \"" + query + "\" found.\nTry a different search keyword.");
            } else {
                emptyPantryView.setText("Your pantry is currently empty.\nTap the '+' button below to add your first ingredient!");
            }
        } else {
            if (emptyPantryContainer != null) {
                emptyPantryContainer.setVisibility(View.GONE);
            }
            emptyPantryView.setVisibility(View.GONE);
            pantryRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void confirmDeleteItem(PantryItem item) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Ingredient")
                .setMessage("Are you sure you want to remove \"" + item.getDisplayName() + "\" from your pantry?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    dbHelper.deletePantryItem(item.getId());
                    Toast.makeText(MainActivity.this, item.getDisplayName() + " deleted", Toast.LENGTH_SHORT).show();
                    loadPantryData();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
