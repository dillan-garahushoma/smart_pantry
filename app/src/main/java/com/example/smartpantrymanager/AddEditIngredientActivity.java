package com.example.smartpantrymanager;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.smartpantrymanager.db.PantryDatabaseHelper;
import com.example.smartpantrymanager.model.PantryItem;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;

public class AddEditIngredientActivity extends AppCompatActivity {
    private PantryDatabaseHelper dbHelper;
    private EditText ingredientNameInput;
    private EditText ingredientQuantityInput;
    private EditText expiryDateInput;
    private Spinner unitSpinner;
    private TextView validationMessage;
    private long currentIngredientId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_ingredient);

        dbHelper = new PantryDatabaseHelper(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Add Ingredient");
        }

        ingredientNameInput = findViewById(R.id.ingredientNameInput);
        ingredientQuantityInput = findViewById(R.id.quantityInput);
        expiryDateInput = findViewById(R.id.expiryDateInput);
        unitSpinner = findViewById(R.id.unitSpinner);
        validationMessage = findViewById(R.id.validationMessage);
        Button pickDateButton = findViewById(R.id.pickDateButton);
        Button saveButton = findViewById(R.id.saveIngredientButton);
        Button cancelButton = findViewById(R.id.cancelButton);

        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                Arrays.asList("pcs", "g", "kg", "ml", "l", "cup", "tbsp", "tsp", "slice", "clove"));
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(unitAdapter);

        pickDateButton.setOnClickListener(v -> showDatePicker());
        cancelButton.setOnClickListener(v -> finish());
        saveButton.setOnClickListener(v -> saveIngredient());

        currentIngredientId = getIntent().getLongExtra("ingredient_id", -1L);
        if (currentIngredientId > 0) {
            populateFormForEditing();
        }
    }

    private void populateFormForEditing() {
        PantryItem item = dbHelper.getPantryItemById(currentIngredientId);
        if (item == null) {
            finish();
            return;
        }

        ingredientNameInput.setText(item.getDisplayName());
        ingredientQuantityInput.setText(String.format(Locale.US, "%.2f", item.getQuantity()));
        expiryDateInput.setText(item.getExpiryDate() == null ? "" : item.getExpiryDate());

        ArrayAdapter<String> adapter = (ArrayAdapter<String>) unitSpinner.getAdapter();
        if (adapter != null) {
            int index = adapter.getPosition(item.getUnit());
            if (index >= 0) {
                unitSpinner.setSelection(index);
            }
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Edit Ingredient");
        }
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog datePicker = new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String selectedDate = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            expiryDateInput.setText(selectedDate);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    private void saveIngredient() {
        String ingredientName = ingredientNameInput.getText() == null ? "" : ingredientNameInput.getText().toString().trim();
        String quantityText = ingredientQuantityInput.getText() == null ? "" : ingredientQuantityInput.getText().toString().trim();
        String selectedUnit = unitSpinner.getSelectedItem() == null ? "" : unitSpinner.getSelectedItem().toString();
        String expiryDate = expiryDateInput.getText() == null ? "" : expiryDateInput.getText().toString().trim();

        boolean valid = true;
        ingredientNameInput.setError(null);
        ingredientQuantityInput.setError(null);

        if (ingredientName.isEmpty()) {
            ingredientNameInput.setError("Ingredient name is required.");
            valid = false;
        }

        if (quantityText.isEmpty()) {
            ingredientQuantityInput.setError("Quantity is required.");
            valid = false;
        }

        double quantityValue;
        try {
            quantityValue = Double.parseDouble(quantityText);
            if (quantityValue <= 0) {
                ingredientQuantityInput.setError("Quantity must be greater than zero.");
                valid = false;
            }
        } catch (NumberFormatException e) {
            ingredientQuantityInput.setError("Enter a valid numeric quantity.");
            valid = false;
        }

        if (selectedUnit == null || selectedUnit.trim().isEmpty()) {
            validationMessage.setText("Please choose a valid unit.");
            valid = false;
        }

        if (!valid) {
            validationMessage.setText("Please fix the highlighted fields.");
            return;
        }

        if (dbHelper.ingredientExists(ingredientName, currentIngredientId)) {
            ingredientNameInput.setError("This ingredient is already in your pantry.");
            validationMessage.setText("Duplicate ingredient detected. Update the existing item instead.");
            return;
        }

        if (expiryDate != null && !expiryDate.isEmpty() && !expiryDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
            expiryDateInput.setError("Use the YYYY-MM-DD date format.");
            validationMessage.setText("Expiry date format is invalid.");
            return;
        }

        PantryItem item = new PantryItem();
        item.setId(currentIngredientId > 0 ? currentIngredientId : 0);
        item.setName(ingredientName);
        item.setDisplayName(ingredientName);
        item.setQuantity(quantityValue);
        item.setUnit(selectedUnit);
        item.setExpiryDate(expiryDate);

        dbHelper.insertOrUpdatePantryItem(item);
        Toast.makeText(this, currentIngredientId > 0 ? "Ingredient updated" : "Ingredient saved", Toast.LENGTH_SHORT).show();
        finish();
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
