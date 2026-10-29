package com.example.smartpantrymanager.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

import com.example.smartpantrymanager.model.PantryItem;
import com.example.smartpantrymanager.model.Recipe;
import com.example.smartpantrymanager.model.RecipeIngredient;
import com.example.smartpantrymanager.service.RecipeMatchingService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PantryDatabaseHelper extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "smart_pantry_manager.db";
    public static final int DATABASE_VERSION = 1;

    public static final String TABLE_PANTRY_ITEMS = "pantry_items";
    public static final String TABLE_RECIPES = "recipes";
    public static final String TABLE_RECIPE_INGREDIENTS = "recipe_ingredients";

    public static final String COL_ID = "id";
    public static final String COL_CANONICAL_NAME = "canonical_name";
    public static final String COL_DISPLAY_NAME = "display_name";
    public static final String COL_QUANTITY = "quantity";
    public static final String COL_UNIT = "unit";
    public static final String COL_EXPIRY_DATE = "expiry_date";

    public PantryDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_PANTRY_ITEMS + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_CANONICAL_NAME + " TEXT NOT NULL, "
                + COL_DISPLAY_NAME + " TEXT, "
                + COL_QUANTITY + " REAL NOT NULL, "
                + COL_UNIT + " TEXT NOT NULL, "
                + COL_EXPIRY_DATE + " TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_RECIPES + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "name TEXT NOT NULL, "
                + "description TEXT, "
                + "preparation_time TEXT, "
                + "instructions TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_RECIPE_INGREDIENTS + " ("
                + "recipe_id INTEGER NOT NULL, "
                + "ingredient_name TEXT NOT NULL, "
                + "quantity REAL NOT NULL, "
                + "unit TEXT NOT NULL)");

        seedRecipesIfNeeded(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPE_INGREDIENTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_RECIPES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PANTRY_ITEMS);
        onCreate(db);
    }

    public void seedRecipesIfNeeded() {
        SQLiteDatabase db = getWritableDatabase();
        seedRecipesIfNeeded(db);
    }

    private void seedRecipesIfNeeded(SQLiteDatabase db) {
        if (countRecipes(db) > 0) {
            return;
        }
        for (Recipe recipe : buildSeedRecipes()) {
            insertRecipe(db, recipe);
        }
    }

    public long insertOrUpdatePantryItem(PantryItem item) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CANONICAL_NAME, RecipeMatchingService.normalizeIngredientName(item.getName()));
        values.put(COL_DISPLAY_NAME, item.getDisplayName());
        values.put(COL_QUANTITY, item.getQuantity());
        values.put(COL_UNIT, item.getUnit());
        values.put(COL_EXPIRY_DATE, item.getExpiryDate());

        if (item.getId() > 0) {
            db.update(TABLE_PANTRY_ITEMS, values, COL_ID + " = ?", new String[]{String.valueOf(item.getId())});
            return item.getId();
        }
        return db.insert(TABLE_PANTRY_ITEMS, null, values);
    }

    public int updatePantryItem(PantryItem item) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_CANONICAL_NAME, RecipeMatchingService.normalizeIngredientName(item.getName()));
        values.put(COL_DISPLAY_NAME, item.getDisplayName());
        values.put(COL_QUANTITY, item.getQuantity());
        values.put(COL_UNIT, item.getUnit());
        values.put(COL_EXPIRY_DATE, item.getExpiryDate());
        return db.update(TABLE_PANTRY_ITEMS, values, COL_ID + " = ?", new String[]{String.valueOf(item.getId())});
    }

    public int deletePantryItem(long id) {
        SQLiteDatabase db = getWritableDatabase();
        return db.delete(TABLE_PANTRY_ITEMS, COL_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public PantryItem getPantryItemById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_PANTRY_ITEMS, null, COL_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);
        if (cursor == null || !cursor.moveToFirst()) {
            if (cursor != null) cursor.close();
            return null;
        }
        PantryItem item = cursorToPantryItem(cursor);
        cursor.close();
        return item;
    }

    public List<PantryItem> getAllPantryItems() {
        return getPantryItems(null, "name");
    }

    public List<PantryItem> getPantryItems(String searchQuery, String sortBy) {
        SQLiteDatabase db = getReadableDatabase();
        StringBuilder queryBuilder = new StringBuilder("SELECT * FROM " + TABLE_PANTRY_ITEMS);
        List<String> selectionArgs = new ArrayList<>();

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            String normalized = "%" + searchQuery.trim().toLowerCase(Locale.US) + "%";
            queryBuilder.append(" WHERE lower(")
                    .append(COL_DISPLAY_NAME).append(" ) LIKE ? OR lower(")
                    .append(COL_CANONICAL_NAME).append(" ) LIKE ?");
            selectionArgs.add(normalized);
            selectionArgs.add(normalized);
        }

        String orderBy = "lower(display_name) ASC";
        if (sortBy != null) {
            switch (sortBy) {
                case "expiry":
                    orderBy = "expiry_date IS NULL, expiry_date ASC, lower(display_name) ASC";
                    break;
                case "quantity":
                    orderBy = "quantity DESC, lower(display_name) ASC";
                    break;
                case "name":
                default:
                    orderBy = "lower(display_name) ASC";
                    break;
            }
        }

        queryBuilder.append(" ORDER BY ").append(orderBy);
        Cursor cursor = db.rawQuery(queryBuilder.toString(), selectionArgs.toArray(new String[0]));
        List<PantryItem> items = new ArrayList<>();
        if (cursor == null) {
            return items;
        }
        while (cursor.moveToNext()) {
            items.add(cursorToPantryItem(cursor));
        }
        cursor.close();
        return items;
    }

    public boolean ingredientExists(String name, long ignoreId) {
        SQLiteDatabase db = getReadableDatabase();
        String canonicalName = RecipeMatchingService.normalizeIngredientName(name);
        Cursor cursor = db.query(TABLE_PANTRY_ITEMS, new String[]{COL_ID}, COL_CANONICAL_NAME + " = ? AND " + COL_ID + " != ?",
                new String[]{canonicalName, String.valueOf(ignoreId)}, null, null, null);
        boolean exists = cursor != null && cursor.moveToFirst();
        if (cursor != null) cursor.close();
        return exists;
    }

    public long insertRecipe(SQLiteDatabase db, Recipe recipe) {
        ContentValues values = new ContentValues();
        values.put("name", recipe.getName());
        values.put("description", recipe.getDescription());
        values.put("preparation_time", recipe.getPreparationTime());
        values.put("instructions", recipe.getInstructions());
        long recipeId = db.insert(TABLE_RECIPES, null, values);

        for (RecipeIngredient ingredient : recipe.getIngredients()) {
            ContentValues ingredientValues = new ContentValues();
            ingredientValues.put("recipe_id", recipeId);
            ingredientValues.put("ingredient_name", ingredient.getIngredientName());
            ingredientValues.put("quantity", ingredient.getQuantity());
            ingredientValues.put("unit", ingredient.getUnit());
            db.insert(TABLE_RECIPE_INGREDIENTS, null, ingredientValues);
        }
        return recipeId;
    }

    public List<Recipe> getAllRecipes() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_RECIPES, null, null, null, null, null, "id ASC");
        List<Recipe> recipes = new ArrayList<>();
        if (cursor == null) {
            return recipes;
        }
        while (cursor.moveToNext()) {
            Recipe recipe = cursorToRecipe(cursor);
            recipe.setIngredients(getIngredientsForRecipe(db, recipe.getId()));
            recipes.add(recipe);
        }
        cursor.close();
        return recipes;
    }

    public Recipe getRecipeById(long recipeId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.query(TABLE_RECIPES, null, COL_ID + " = ?", new String[]{String.valueOf(recipeId)}, null, null, null);
        if (cursor == null || !cursor.moveToFirst()) {
            if (cursor != null) cursor.close();
            return null;
        }
        Recipe recipe = cursorToRecipe(cursor);
        cursor.close();
        recipe.setIngredients(getIngredientsForRecipe(db, recipeId));
        return recipe;
    }

    public int countRecipes() {
        return countRecipes(getReadableDatabase());
    }

    private int countRecipes(SQLiteDatabase db) {
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_RECIPES, null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    @NonNull
    private PantryItem cursorToPantryItem(Cursor cursor) {
        PantryItem item = new PantryItem();
        item.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)));
        item.setName(cursor.getString(cursor.getColumnIndexOrThrow(COL_CANONICAL_NAME)));
        item.setDisplayName(cursor.getString(cursor.getColumnIndexOrThrow(COL_DISPLAY_NAME)));
        item.setQuantity(cursor.getDouble(cursor.getColumnIndexOrThrow(COL_QUANTITY)));
        item.setUnit(cursor.getString(cursor.getColumnIndexOrThrow(COL_UNIT)));
        item.setExpiryDate(cursor.getString(cursor.getColumnIndexOrThrow(COL_EXPIRY_DATE)));
        return item;
    }

    @NonNull
    private Recipe cursorToRecipe(Cursor cursor) {
        Recipe recipe = new Recipe();
        recipe.setId(cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)));
        recipe.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
        recipe.setDescription(cursor.getString(cursor.getColumnIndexOrThrow("description")));
        recipe.setPreparationTime(cursor.getString(cursor.getColumnIndexOrThrow("preparation_time")));
        recipe.setInstructions(cursor.getString(cursor.getColumnIndexOrThrow("instructions")));
        return recipe;
    }

    private List<RecipeIngredient> getIngredientsForRecipe(SQLiteDatabase db, long recipeId) {
        Cursor cursor = db.query(TABLE_RECIPE_INGREDIENTS, null, "recipe_id = ?", new String[]{String.valueOf(recipeId)}, null, null, null);
        List<RecipeIngredient> ingredients = new ArrayList<>();
        if (cursor == null) {
            return ingredients;
        }
        while (cursor.moveToNext()) {
            RecipeIngredient ingredient = new RecipeIngredient();
            ingredient.setRecipeId(recipeId);
            ingredient.setIngredientName(cursor.getString(cursor.getColumnIndexOrThrow("ingredient_name")));
            ingredient.setQuantity(cursor.getDouble(cursor.getColumnIndexOrThrow("quantity")));
            ingredient.setUnit(cursor.getString(cursor.getColumnIndexOrThrow("unit")));
            ingredients.add(ingredient);
        }
        cursor.close();
        return ingredients;
    }

    private List<Recipe> buildSeedRecipes() {
        List<Recipe> recipes = new ArrayList<>();

        addRecipe(recipes, "Garden Omelette", "Quick breakfast with spinach and peppers.", "15 minutes",
                "1. Whisk the eggs until smooth.\n2. Sauté onion and spinach with pepper.\n3. Pour in eggs and fold until set.",
                new String[]{"egg:2:pcs", "spinach:1:cup", "onion:1:pcs", "pepper:1:pcs"});
        addRecipe(recipes, "Tomato Garlic Pasta", "A comforting tomato sauce with garlic.", "25 minutes",
                "1. Boil the pasta until tender.\n2. Sauté garlic and tomato in olive oil.\n3. Toss with pasta and serve.",
                new String[]{"pasta:200:g", "tomato:2:pcs", "garlic:2:cloves", "olive oil:1:tbsp"});
        addRecipe(recipes, "Egg Fried Rice", "A savory rice dish with eggs and veggies.", "20 minutes",
                "1. Cook rice until fluffy.\n2. Scramble eggs and onion.\n3. Mix with rice and pepper.",
                new String[]{"rice:150:g", "egg:2:pcs", "onion:1:pcs", "pepper:1:pcs"});
        addRecipe(recipes, "Rainbow Bean Salad", "Colorful beans with tomatoes and greens.", "15 minutes",
                "1. Rinse beans and vegetables.\n2. Chop tomato, onion, and spinach.\n3. Toss together lightly.",
                new String[]{"beans:1:cup", "tomato:2:pcs", "onion:1:pcs", "spinach:1:cup"});
        addRecipe(recipes, "Garlic Pepper Stir-Fry", "Fast vegetable stir-fry with garlic and rice.", "18 minutes",
                "1. Slice vegetables thinly.\n2. Cook garlic and pepper.\n3. Stir in rice and onions.",
                new String[]{"garlic:3:cloves", "pepper:2:pcs", "onion:1:pcs", "rice:100:g"});
        addRecipe(recipes, "Cheesy Potato Bake", "Hearty potato layers with dairy and cheese.", "35 minutes",
                "1. Slice potatoes thinly.\n2. Layer with cheese and milk.\n3. Bake until golden.",
                new String[]{"potato:3:pcs", "cheese:150:g", "milk:200:ml", "butter:1:tbsp"});
        addRecipe(recipes, "Carrot Soup", "Smooth soup with carrot, onion, and creaminess.", "30 minutes",
                "1. Sauté onion and carrot.\n2. Add milk and simmer.\n3. Blend until smooth.",
                new String[]{"carrot:3:pcs", "onion:1:pcs", "milk:300:ml", "butter:1:tbsp"});
        addRecipe(recipes, "Spinach Rice Bowl", "Rice topped with garlic spinach and olive oil.", "20 minutes",
                "1. Warm the rice.\n2. Sauté spinach and garlic.\n3. Toss together with olive oil.",
                new String[]{"rice:150:g", "spinach:2:cup", "garlic:2:cloves", "olive oil:1:tbsp"});
        addRecipe(recipes, "Banana Toast", "Simple fruit toast with yogurt.", "10 minutes",
                "1. Toast the bread lightly.\n2. Top with banana slices and yogurt.\n3. Serve immediately.",
                new String[]{"bread:2:slices", "banana:1:pcs", "yogurt:50:g"});
        addRecipe(recipes, "Fluffy Pancakes", "Classic pancakes made from pantry staples.", "25 minutes",
                "1. Mix flour, milk, and egg.\n2. Cook in hot pan.\n3. Serve warm.",
                new String[]{"flour:100:g", "milk:200:ml", "egg:2:pcs", "butter:1:tbsp"});
        addRecipe(recipes, "Stuffed Tomatoes", "Tomato boats filled with rice and onion.", "20 minutes",
                "1. Scoop tomatoes.\n2. Fill with rice and onion.\n3. Bake until warm.",
                new String[]{"tomato:2:pcs", "rice:100:g", "onion:1:pcs", "garlic:2:cloves"});
        addRecipe(recipes, "Grilled Cheese Toast", "Toasted bread with melted cheese and butter.", "12 minutes",
                "1. Butter the bread lightly.\n2. Add cheese and toast.\n3. Serve warm.",
                new String[]{"bread:2:slices", "cheese:100:g", "butter:1:tbsp"});
        addRecipe(recipes, "Tomato Basil Soup", "Warm soup featuring tomato and onion.", "30 minutes",
                "1. Simmer tomato and onion.\n2. Add milk.\n3. Blend and serve.",
                new String[]{"tomato:3:pcs", "onion:1:pcs", "milk:300:ml"});
        addRecipe(recipes, "Bean and Pepper Toast", "Savory toast with beans, pepper, and onion.", "12 minutes",
                "1. Warm the beans.\n2. Stir in onion and pepper.\n3. Spoon onto toast.",
                new String[]{"beans:1:cup", "pepper:1:pcs", "onion:1:pcs", "bread:2:slices"});
        addRecipe(recipes, "Spinach Egg Wrap", "Portable breakfast wrap with spinach and egg.", "15 minutes",
                "1. Scramble eggs with spinach and onion.\n2. Fill the wrap.\n3. Serve warm.",
                new String[]{"egg:2:pcs", "spinach:1:cup", "onion:1:pcs", "bread:1:slice"});
        addRecipe(recipes, "Tomato Rice Skillet", "Fresh tomato skillet with herbs and rice.", "22 minutes",
                "1. Sauté onion and tomato.\n2. Add rice and stir.\n3. Finish with seasoning.",
                new String[]{"tomato:2:pcs", "rice:150:g", "onion:1:pcs", "olive oil:1:tbsp"});
        addRecipe(recipes, "Potato Egg Hash", "Comforting potato hash with egg and onion.", "18 minutes",
                "1. Fry potato pieces until golden.\n2. Add onion and egg.\n3. Cook until set.",
                new String[]{"potato:2:pcs", "egg:2:pcs", "onion:1:pcs"});
        addRecipe(recipes, "Cheesy Tomato Toast", "Loaded toast with melted cheese and tomato.", "14 minutes",
                "1. Toast the bread.\n2. Top with tomato and cheese.\n3. Grill until melted.",
                new String[]{"bread:2:slices", "tomato:2:pcs", "cheese:80:g"});
        addRecipe(recipes, "Rice and Bean Bowl", "A filling bowl with beans, rice and greens.", "20 minutes",
                "1. Warm beans and rice together.\n2. Add spinach and onion.\n3. Serve with seasoning.",
                new String[]{"rice:150:g", "beans:1:cup", "spinach:1:cup", "onion:1:pcs"});

        return recipes;
    }

    private void addRecipe(List<Recipe> recipes, String name, String description, String time, String instructions, String[] ingredientSpecs) {
        Recipe recipe = new Recipe();
        recipe.setName(name);
        recipe.setDescription(description);
        recipe.setPreparationTime(time);
        recipe.setInstructions(instructions);

        List<RecipeIngredient> ingredients = new ArrayList<>();
        for (String entry : ingredientSpecs) {
            String[] parts = entry.split(":");
            RecipeIngredient ingredient = new RecipeIngredient();
            ingredient.setIngredientName(parts[0]);
            ingredient.setQuantity(Double.parseDouble(parts[1]));
            ingredient.setUnit(parts[2]);
            ingredients.add(ingredient);
        }
        recipe.setIngredients(ingredients);
        recipes.add(recipe);
    }
}
