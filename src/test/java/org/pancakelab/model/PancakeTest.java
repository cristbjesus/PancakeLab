package org.pancakelab.model;

import org.junit.jupiter.api.Test;
import org.pancakelab.exception.DuplicateIngredientException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PancakeTest {
    @Test
    public void GivenPlainPancake_WhenDescribing_ThenDescriptionHasNoIngredients() {
        // exercise
        String description = Pancake.plain().description();

        // verify
        assertEquals("Delicious pancake!", description);
    }

    @Test
    public void GivenIngredientsAdded_WhenDescribing_ThenDescriptionListsIngredientsInOrder() {
        // setup
        Pancake pancake = Pancake.plain()
                .with(new Ingredient("milk chocolate"))
                .with(new Ingredient("dark chocolate"))
                .with(new Ingredient("hazelnuts"));

        // exercise
        String description = pancake.description();

        // verify
        assertEquals("Delicious pancake with milk chocolate, dark chocolate, hazelnuts!", description);
    }

    @Test
    public void GivenPancake_WhenAddingIngredient_ThenOriginalPancakeUnchanged() {
        // setup
        Pancake original = Pancake.plain();

        // exercise
        original.with(new Ingredient("dark chocolate"));

        // verify
        assertEquals("Delicious pancake!", original.description());
    }

    @Test
    public void GivenIngredientAlreadyOnPancake_WhenAddingItAgain_ThenDuplicateIngredientExceptionThrown() {
        // setup
        Pancake pancake = Pancake.plain().with(new Ingredient("dark chocolate"));

        // exercise
        DuplicateIngredientException exception =
                assertThrows(DuplicateIngredientException.class, () -> pancake.with(new Ingredient("dark chocolate")));

        // verify
        assertEquals("dark chocolate", exception.getIngredient());
    }
}
