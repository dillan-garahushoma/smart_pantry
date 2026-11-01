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
        Collections.addAll(COUNT_UNITS, "pcs", "pc", "piece", "pieces", "count", "item", "items",
                "whole", "wholeitem", "egg", "eggs", "slice", "slices", "clove", "cloves", "bunch", "bunches");

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
        CANONICAL_ALIASES.put("bell pepper", "pepper");
        CANONICAL_ALIASES.put("bell peppers", "pepper");
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
        CANONICAL_ALIASES.put("oil", "olive oil");
        CANONICAL_ALIASES.put("salt", "salt");
        CANONICAL_ALIASES.put("peppercorns", "pepper");
        CANONICAL_ALIASES.put("yogurt", "yogurt");
        CANONICAL_ALIASES.put("banana", "banana");
        CANONICAL_ALIASES.put("bananas", "banana");
    }

    /**
     * Evaluation result for a single recipe matching against pantry.
     */
    public static class RecipeMatchResult {
        private final Recipe recipe;
        private final int totalRequired;
        private final int matchedCount;
        private final List<RecipeIngredient> missingIngredients;
        private final List<RecipeIngredient> satisfiedIngredients;

        public RecipeMatchResult(Recipe recipe, int totalRequired, int matchedCount,
                                 List<RecipeIngredient> missingIngredients,
                                 List<RecipeIngredient> satisfiedIngredients) {
            this.recipe = recipe;
            this.totalRequired = totalRequired;
            this.matchedCount = matchedCount;
            this.missingIngredients = missingIngredients;
            this.satisfiedIngredients = satisfiedIngredients;
        }

        public Recipe getRecipe() {
            return recipe;
        }

        public int getTotalRequired() {
            return totalRequired;
        }

        public int getMatchedCount() {
            return matchedCount;
        }

        public List<RecipeIngredient> getMissingIngredients() {
            return missingIngredients;
        }

        public List<RecipeIngredient> getSatisfiedIngredients() {
            return satisfiedIngredients;
        }

        public boolean isStrictMatch() {
            return missingIngredients.isEmpty() && totalRequired > 0;
        }

        public boolean isAlmostThere() {
            return missingIngredients.size() == 1;
        }

        public String getMatchBadgeText() {
            if (isStrictMatch()) {
                return "Ready to cook";
            } else if (isAlmostThere()) {
                RecipeIngredient missing = missingIngredients.get(0);
                return "Missing: " + missing.getIngredientName();
            } else {
                return matchedCount + "/" + totalRequired + " in pantry";
            }
        }
    }

    /**
     * Strict matching (Section 2.3): Only recipes where 100% of ingredients are in pantry with sufficient quantity.
     */
    public static List<Recipe> getEligibleRecipes(List<Recipe> recipes, List<PantryItem> pantryItems) {
        if (recipes == null) {
            return new ArrayList<>();
        }
        List<Recipe> eligible = new ArrayList<>();
        for (Recipe recipe : recipes) {
            RecipeMatchResult result = evaluateRecipe(recipe, pantryItems);
            if (result.isStrictMatch()) {
                eligible.add(recipe);
            }
        }
        return eligible;
    }

    /**
     * Almost-There matching (Section 2.3 & 8 bonus stretch): Recipes missing exactly 1 ingredient.
     */
    public static List<RecipeMatchResult> getAlmostThereMatches(List<Recipe> recipes, List<PantryItem> pantryItems) {
        if (recipes == null) {
            return new ArrayList<>();
        }
        List<RecipeMatchResult> almostThere = new ArrayList<>();
        for (Recipe recipe : recipes) {
            RecipeMatchResult result = evaluateRecipe(recipe, pantryItems);
            if (result.isAlmostThere()) {
                almostThere.add(result);
            }
        }
        return almostThere;
    }

    /**
     * Evaluates all recipes with their detailed matching metrics.
     */
    public static List<RecipeMatchResult> getAllRecipeMatches(List<Recipe> recipes, List<PantryItem> pantryItems) {
        if (recipes == null) {
            return new ArrayList<>();
        }
        List<RecipeMatchResult> results = new ArrayList<>();
        for (Recipe recipe : recipes) {
            results.add(evaluateRecipe(recipe, pantryItems));
        }
        return results;
    }

    public static boolean isRecipeEligible(Recipe recipe, List<PantryItem> pantryItems) {
        RecipeMatchResult result = evaluateRecipe(recipe, pantryItems);
        return result.isStrictMatch();
    }

    public static RecipeMatchResult evaluateRecipe(Recipe recipe, List<PantryItem> pantryItems) {
        if (recipe == null || recipe.getIngredients() == null || recipe.getIngredients().isEmpty()) {
            return new RecipeMatchResult(recipe, 0, 0, new ArrayList<>(), new ArrayList<>());
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

        List<RecipeIngredient> missing = new ArrayList<>();
        List<RecipeIngredient> satisfied = new ArrayList<>();

        for (RecipeIngredient required : recipe.getIngredients()) {
            if (required == null || required.getIngredientName() == null || required.getIngredientName().trim().isEmpty()) {
                continue;
            }
            String requiredKey = normalizeIngredientName(required.getIngredientName());
            PantryItem item = pantryMap.get(requiredKey);
            if (item != null && hasSufficientQuantity(required, item)) {
                satisfied.add(required);
            } else {
                missing.add(required);
            }
        }

        return new RecipeMatchResult(recipe, recipe.getIngredients().size(), satisfied.size(), missing, satisfied);
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

        String ingredientName = normalizeIngredientName(required.getIngredientName());

        // Smart cross-unit bridge for realistic user inputs (e.g. egg entered in grams vs count)
        if (ingredientName.equals("egg") && !areUnitsCompatible(requiredUnit, availableUnit)) {
            if (COUNT_UNITS.contains(requiredUnit) && availableUnit.equals("g")) {
                // 1 egg ~ 50g average weight
                double eggsAvailable = availableQty / 50.0;
                return eggsAvailable >= (requiredQty * 0.8); // allow reasonable leeway
            } else if (requiredUnit.equals("g") && COUNT_UNITS.contains(availableUnit)) {
                return (availableQty * 50.0) >= requiredQty;
            }
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
        if (COUNT_UNITS.contains(normalized)) {
            return "pcs";
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
