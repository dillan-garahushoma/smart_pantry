package com.example.smartpantrymanager;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;

import com.example.smartpantrymanager.db.PantryDatabaseHelper;

import java.util.Arrays;

public class SettingsActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "smart_pantry_settings";
    private static final String KEY_EXPIRING_ALERTS = "expiring_alerts";
    private static final String KEY_DISPLAY_UNIT = "display_unit";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        SwitchCompat alertsSwitch = findViewById(R.id.expiringSoonSwitch);
        Spinner unitSpinner = findViewById(R.id.displayUnitSpinner);
        Button reloadRecipesButton = findViewById(R.id.reloadRecipesButton);
        TextView appInfoText = findViewById(R.id.appInfoText);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        alertsSwitch.setChecked(prefs.getBoolean(KEY_EXPIRING_ALERTS, true));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
                Arrays.asList("Default", "Metric", "Imperial"));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        unitSpinner.setAdapter(adapter);

        String savedUnit = prefs.getString(KEY_DISPLAY_UNIT, "Metric");
        int savedIndex = adapter.getPosition(savedUnit);
        if (savedIndex >= 0) {
            unitSpinner.setSelection(savedIndex);
        }

        alertsSwitch.setOnCheckedChangeListener((buttonView, isChecked) ->
                prefs.edit().putBoolean(KEY_EXPIRING_ALERTS, isChecked).apply());

        unitSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                prefs.edit().putString(KEY_DISPLAY_UNIT, parent.getItemAtPosition(position).toString()).apply();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        reloadRecipesButton.setOnClickListener(v -> {
            PantryDatabaseHelper dbHelper = new PantryDatabaseHelper(this);
            dbHelper.seedRecipesIfNeeded();
            Toast.makeText(this, "Recipe collection reloaded.", Toast.LENGTH_SHORT).show();
        });

        appInfoText.setText("Smart Pantry Manager\nVersion 1.0\nSQLite-backed pantry and recipe matching.");
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
