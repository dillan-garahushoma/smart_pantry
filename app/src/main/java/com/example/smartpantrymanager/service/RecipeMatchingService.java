package com.example.smartpantrymanager.service;

import com.example.smartpantrymanager.model.PantryItem;
import com.example.smartpantrymanager.model.Recipe;
import com.example.smartpantrymanager.model.RecipeIngredient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class RecipeMatchingService {
    private static final Set<String> COUNT_UNITS = new HashSet<>();
    private static final Map<String, String> CANONICAL_ALIASES = new HashMap<>();

    static {
        Collections.addAll(COUNT_UNITS, "pcs", "pc", "piece", "pieces", "count", "whole", "wholeitem", "egg", "eggs", "slice", "slices", "clove", "cloves", "bunch", "bunches");

        CANONICAL_ALIASES.put("tomato", "tomato");
        CANONICAL_ALIASES.put("tomatoes", "tomato");
        CANONICAL_ALIASES.put("egg", "egg");
        CANONICAL_ALIASES.put("eggs", "egg");
        CANONICAL_ALIASES.put("flour", "flour");
        CANONICAL_ALIASES.put("all purpose flour", "flour");
        CANONICAL_ALIASES.put("milk", "milk");
        CANONICAL_ALIASES.put("onion", "onion");
        CANONICAL_ALIASES.put("onions", "onion");
        CANONICAL_ALIASES.put("garlic", "garlic");
        CANONICAL_ALIASES.put("garlic cloves", "garlic");
        CANONICAL_ALIASES.put("spinach", "spinach");
        CANONICAL_ALIASES.put("pepper", "pepper");
        CANONICAL_ALIASES.put("peppers", "pepper");
        CANONICAL_ALIASES.put("chilies", "chili");
        CANONICAL_ALIASES.put("chili", "chili");
        CANONICAL_ALIASES.put("chilli", "chili");
        CANONICAL_ALIASES.put("carrot", "carrot");
        CANONICAL_ALIASES.put("carrots", "carrot");
        CANONICAL_ALIASES.put("potato", "potato");
        CANONICAL_ALIASES.put("potatoes", "potato");
        CANONICAL_ALIASES.put("bread", "bread");
        CANONICAL_ALIASES.put("rice", "rice");
        CANONICAL_ALIASES.put("pasta", "pasta");
        CANONICAL_ALIASES.put("beans", "beans");
        CANONICAL_ALIASES.put("bean", "beans");
        CANONICAL_ALIASES.put("cheese", "cheese");
        CANONICAL_ALIASES.put("butter", "butter");
        CANONICAL_ALIASES.put("olive oil", "olive oil");
        CANONICAL_ALIASES.put("oil", "oil");
        CANONICAL_ALIASES.put("salt", "salt");
        CANONICAL_ALIASES.put("peppercorns", "pepper");
    }

    public static List<Recipe> getEligibleRecipes(List<Recipe> recipes, List<PantryItem> pantryItems) {
        if (recipes == null) {
            return new ArrayList<>();
        }
        List<Recipe> eligible = new ArrayList<>();
        for (Recipe recipe : recipes) {
            if (isRecipeEligible(recipe, pantryItems)) {
                eligible.add(recipe);
            }
        }
        return eligible;
    }

    public static boolean isRecipeEligible(Recipe recipe, List<PantryItem> pantryItems) {
        if (recipe == null || recipe.getIngredients() == null || recipe.getIngredients().isEmpty()) {
            return false;
        }
        Map<String, PantryItem> pantryMap = new HashMap<>();
        if (pantryItems != null) {
            for (PantryItem pantryItem : pantryItems) {
                if (pantryItem == null || pantryItem.getName() == null || pantryItem.getName().trim().isEmpty()) {
                    continue;
                }
                String normalized = normalizeIngredientName(pantryItem.getName());
                pantryMap.put(normalized, pantryItem);
            }
        }

        for (RecipeIngredient required : recipe.getIngredients()) {
            if (required == null || required.getIngredientName() == null || required.getIngredientName().trim().isEmpty()) {
                return false;
            }
            String requiredKey = normalizeIngredientName(required.getIngredientName());
            PantryItem item = pantryMap.get(requiredKey);
            if (item == null) {
                return false;
            }
            if (!hasSufficientQuantity(required, item)) {
                return false;
            }
        }
        return true;
    }

    public static String normalizeIngredientName(String rawName) {
        if (rawName == null) {
            return "";
        }
        String cleaned = rawName.trim().toLowerCase(Locale.US);
        cleaned = cleaned.replaceAll("\\s+", " ");
        cleaned = cleaned.replace("-", " ");
        cleaned = cleaned.replace("_", " ");
        cleaned = cleaned.replace(".", "");

        if (CANONICAL_ALIASES.containsKey(cleaned)) {
            return CANONICAL_ALIASES.get(cleaned);
        }
        String singular = cleaned;
        if (cleaned.endsWith("ies") && cleaned.length() > 3) {
            singular = cleaned.substring(0, cleaned.length() - 3) + "y";
        } else if (cleaned.endsWith("es") && cleaned.length() > 3) {
            singular = cleaned.substring(0, cleaned.length() - 2);
        } else if (cleaned.endsWith("s") && !cleaned.endsWith("ss") && cleaned.length() > 2) {
            singular = cleaned.substring(0, cleaned.length() - 1);
        }
        if (CANONICAL_ALIASES.containsKey(singular)) {
            return CANONICAL_ALIASES.get(singular);
        }
        return singular;
    }

    public static boolean hasSufficientQuantity(RecipeIngredient required, PantryItem available) {
        if (required == null || available == null) {
            return false;
        }
        String requiredUnit = normalizeUnit(required.getUnit());
        String availableUnit = normalizeUnit(available.getUnit());
        Double requiredQty = required.getQuantity();
        Double availableQty = available.getQuantity();

        if (requiredQty == null || availableQty == null) {
            return false;
        }
        if (requiredQty <= 0 || availableQty <= 0) {
            return false;
        }
        if (!areUnitsCompatible(requiredUnit, availableUnit)) {
            return false;
        }
        double convertedAvailable = convertToBaseUnit(availableQty, availableUnit);
        double convertedRequired = convertToBaseUnit(requiredQty, requiredUnit);
        return convertedAvailable >= convertedRequired;
    }

    public static boolean areUnitsCompatible(String leftUnit, String rightUnit) {
        if (leftUnit == null || rightUnit == null) {
            return false;
        }
        String normalizedLeft = normalizeUnit(leftUnit);
        String normalizedRight = normalizeUnit(rightUnit);
        if (normalizedLeft.equals(normalizedRight)) {
            return true;
        }
        if (COUNT_UNITS.contains(normalizedLeft) && COUNT_UNITS.contains(normalizedRight)) {
            return true;
        }
        if (isMassUnit(normalizedLeft) && isMassUnit(normalizedRight)) {
            return true;
        }
        if (isVolumeUnit(normalizedLeft) && isVolumeUnit(normalizedRight)) {
            return true;
        }
        return false;
    }

    public static double convertToBaseUnit(double value, String unit) {
        String normalized = normalizeUnit(unit);
        if (COUNT_UNITS.contains(normalized)) {
            return value;
        }
        if (normalized.equals("kg")) {
            return value * 1000d;
        }
        if (normalized.equals("g")) {
            return value;
        }
        if (normalized.equals("mg")) {
            return value / 1000d;
        }
        if (normalized.equals("l")) {
            return value * 1000d;
        }
        if (normalized.equals("ml")) {
            return value;
        }
        if (normalized.equals("cup")) {
            return value * 240d;
        }
        if (normalized.equals("tbsp")) {
            return value * 15d;
        }
        if (normalized.equals("tsp")) {
            return value * 5d;
        }
        return value;
    }

    public static String normalizeUnit(String unit) {
        if (unit == null) {
            return "";
        }
        String normalized = unit.trim().toLowerCase(Locale.US);
        if (normalized.equals("grams") || normalized.equals("gram") || normalized.equals("g")) {
            return "g";
        }
        if (normalized.equals("kilograms") || normalized.equals("kilogram") || normalized.equals("kg")) {
            return "kg";
        }
        if (normalized.equals("milligrams") || normalized.equals("milligram") || normalized.equals("mg")) {
            return "mg";
        }
        if (normalized.equals("liters") || normalized.equals("liter") || normalized.equals("l")) {
            return "l";
        }
        if (normalized.equals("milliliters") || normalized.equals("milliliter") || normalized.equals("ml")) {
            return "ml";
        }
        if (normalized.equals("cups") || normalized.equals("cup")) {
            return "cup";
        }
        if (normalized.equals("tablespoons") || normalized.equals("tablespoon") || normalized.equals("tbsp")) {
            return "tbsp";
        }
        if (normalized.equals("teaspoons") || normalized.equals("teaspoon") || normalized.equals("tsp")) {
            return "tsp";
        }
        if (normalized.equals("pcs") || normalized.equals("pc") || normalized.equals("piece") || normalized.equals("pieces") || normalized.equals("count") || normalized.equals("whole") || normalized.equals("egg") || normalized.equals("eggs") || normalized.equals("slice") || normalized.equals("slices") || normalized.equals("clove") || normalized.equals("cloves") || normalized.equals("bunch") || normalized.equals("bunches")) {
            return normalized;
        }
        return normalized;
    }

    private static boolean isMassUnit(String unit) {
        return unit.equals("g") || unit.equals("kg") || unit.equals("mg");
    }

    private static boolean isVolumeUnit(String unit) {
        return unit.equals("ml") || unit.equals("l") || unit.equals("cup") || unit.equals("tbsp") || unit.equals("tsp");
    }
}
