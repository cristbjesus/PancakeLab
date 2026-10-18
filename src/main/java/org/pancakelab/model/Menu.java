package org.pancakelab.model;

import org.pancakelab.exception.UnknownIngredientException;

import java.util.Set;

public class Menu {
    private final Set<String> ingredients;

    public Menu(Set<String> ingredients) {
        this.ingredients = Set.copyOf(ingredients);
    }

    public Ingredient ingredient(String name) {
        if (name == null || !ingredients.contains(name)) {
            throw new UnknownIngredientException(name);
        }
        return new Ingredient(name);
    }
}
