package org.pancakelab.model;

import org.pancakelab.exception.DuplicateIngredientException;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Pancake {
    private final List<Ingredient> ingredients;

    private Pancake(List<Ingredient> ingredients) {
        this.ingredients = List.copyOf(ingredients);
    }

    public static Pancake plain() {
        return new Pancake(List.of());
    }

    public Pancake with(Ingredient ingredient) {
        if (ingredients.contains(ingredient)) {
            throw new DuplicateIngredientException(ingredient.name());
        }
        List<Ingredient> newIngredients = new ArrayList<>(ingredients);
        newIngredients.add(ingredient);
        return new Pancake(newIngredients);
    }

    public String description() {
        if (ingredients.isEmpty()) {
            return "Delicious pancake!";
        }
        String names = ingredients.stream().map(Ingredient::name).collect(Collectors.joining(", "));
        return "Delicious pancake with %s!".formatted(names);
    }
}
