package org.pancakelab.exception;

public class DuplicateIngredientException extends PancakeLabException {
    private final String ingredient;

    public DuplicateIngredientException(String ingredient) {
        super("Ingredient '%s' is already on the pancake.".formatted(ingredient));
        this.ingredient = ingredient;
    }

    public String getIngredient() {
        return ingredient;
    }
}
