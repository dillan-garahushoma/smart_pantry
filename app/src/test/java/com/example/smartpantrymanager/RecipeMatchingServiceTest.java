package com.example.smartpantrymanager;

import com.example.smartpantrymanager.model.PantryItem;
import com.example.smartpantrymanager.model.Recipe;
import com.example.smartpantrymanager.model.RecipeIngredient;
import com.example.smartpantrymanager.service.RecipeMatchingService;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RecipeMatchingServiceTest {
    @Test
    public void recipeWithEnoughEggsIsEligible() {
        Recipe recipe = recipeWithIngredients("egg", 2, "pcs");
        List<PantryItem> pantry = pantryWithItems("egg", 3, "pcs");
        assertTrue(RecipeMatchingService.isRecipeEligible(recipe, pantry));
    }

    @Test
    public void recipeWithNotEnoughEggsIsExcluded() {
        Recipe recipe = recipeWithIngredients("egg", 2, "pcs");
        List<PantryItem> pantry = pantryWithItems("egg", 1, "pcs");
        assertFalse(RecipeMatchingService.isRecipeEligible(recipe, pantry));
    }

    @Test
    public void missingIngredientExcludesRecipe() {
        Recipe recipe = recipeWithIngredients("egg", 2, "pcs", "flour", 100, "g");
        List<PantryItem> pantry = pantryWithItems("egg", 3, "pcs");
        assertFalse(RecipeMatchingService.isRecipeEligible(recipe, pantry));
    }

    @Test
    public void fullySatisfiedRecipeWithMultipleIngredientsIsEligible() {
        Recipe recipe = recipeWithIngredients("egg", 2, "pcs", "flour", 100, "g");
        List<PantryItem> pantry = pantryWithItems("egg", 3, "pcs", "flour", 150, "g");
        assertTrue(RecipeMatchingService.isRecipeEligible(recipe, pantry));
    }

    @Test
    public void caseAndPluralDifferencesDoNotBreakMatching() {
        Recipe recipe = recipeWithIngredients("Eggs", 2, "pcs");
        List<PantryItem> pantry = pantryWithItems("EGG", 3, "pcs");
        assertTrue(RecipeMatchingService.isRecipeEligible(recipe, pantry));
    }

    @Test
    public void unsupportedUnitsAreRejected() {
        Recipe recipe = recipeWithIngredients("flour", 100, "g");
        List<PantryItem> pantry = pantryWithItems("flour", 100, "ml");
        assertFalse(RecipeMatchingService.isRecipeEligible(recipe, pantry));
    }

    @Test
    public void emptyPantryRejectsRecipe() {
        Recipe recipe = recipeWithIngredients("egg", 2, "pcs");
        assertFalse(RecipeMatchingService.isRecipeEligible(recipe, new ArrayList<>()));
    }

    @Test
    public void multipleRecipesAreFilteredCorrectly() {
        Recipe eligible = recipeWithIngredients("egg", 2, "pcs", "tomato", 2, "pcs");
        Recipe excluded = recipeWithIngredients("egg", 3, "pcs", "milk", 250, "ml");
        List<PantryItem> pantry = pantryWithItems("egg", 3, "pcs", "tomato", 1, "pcs");
        assertTrue(RecipeMatchingService.isRecipeEligible(eligible, pantry));
        assertFalse(RecipeMatchingService.isRecipeEligible(excluded, pantry));
    }

    private Recipe recipeWithIngredients(String... ingredients) {
        Recipe recipe = new Recipe();
        recipe.setIngredients(new ArrayList<>());
        for (int i = 0; i < ingredients.length; i += 3) {
            RecipeIngredient ingredient = new RecipeIngredient();
            ingredient.setIngredientName(ingredients[i]);
            ingredient.setQuantity(Double.parseDouble(ingredients[i + 1]));
            ingredient.setUnit(ingredients[i + 2]);
            recipe.getIngredients().add(ingredient);
        }
        return recipe;
    }

    private List<PantryItem> pantryWithItems(String... ingredients) {
        List<PantryItem> pantry = new ArrayList<>();
        for (int i = 0; i < ingredients.length; i += 3) {
            PantryItem item = new PantryItem();
            item.setName(ingredients[i]);
            item.setDisplayName(ingredients[i]);
            item.setQuantity(Double.parseDouble(ingredients[i + 1]));
            item.setUnit(ingredients[i + 2]);
            pantry.add(item);
        }
        return pantry;
    }
}
