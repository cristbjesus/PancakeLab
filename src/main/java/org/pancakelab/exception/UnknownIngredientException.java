package org.pancakelab.exception;

public class UnknownIngredientException extends PancakeLabException {
    private final String ingredient;

    public UnknownIngredientException(String ingredient) {
        super("Ingredient '%s' is not on the menu.".formatted(ingredient));
        this.ingredient = ingredient;
    }

    public String getIngredient() {
        return ingredient;
    }
}
