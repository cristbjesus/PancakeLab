package org.pancakelab.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.pancakelab.exception.UnknownIngredientException;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class MenuTest {
    private final Menu menu = new Menu(Set.of("dark chocolate", "milk chocolate", "whipped cream", "hazelnuts"));

    @ParameterizedTest
    @ValueSource(strings = {"dark chocolate", "milk chocolate", "whipped cream", "hazelnuts"})
    public void GivenIngredientOnMenu_WhenRequestingIngredient_ThenIngredientReturned(String name) {
        // exercise
        Ingredient ingredient = menu.ingredient(name);

        // verify
        assertEquals(name, ingredient.name());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"mustard", "ketchup", ""})
    public void GivenIngredientNotOnMenu_WhenRequestingIngredient_ThenUnknownIngredientExceptionThrown(String name) {
        // exercise
        UnknownIngredientException exception =
                assertThrows(UnknownIngredientException.class, () -> menu.ingredient(name));

        // verify
        assertEquals(name, exception.getIngredient());
    }

    @Test
    public void GivenIngredientAddedToSourceSetLater_WhenRequestingIngredient_ThenUnknownIngredientExceptionThrown() {
        // setup
        Set<String> ingredients = new HashSet<>(Set.of("dark chocolate"));
        Menu menuFromMutableSet = new Menu(ingredients);
        ingredients.add("mustard");

        // exercise & verify
        assertThrows(UnknownIngredientException.class, () -> menuFromMutableSet.ingredient("mustard"));
    }
}
