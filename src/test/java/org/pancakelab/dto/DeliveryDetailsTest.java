package org.pancakelab.dto;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DeliveryDetailsTest {
    @Test
    public void GivenSourceListChangedAfterCreation_WhenReadingPancakes_ThenDeliveryDetailsUnchanged() {
        // setup
        List<String> pancakes = new ArrayList<>(List.of("Delicious pancake!"));
        DeliveryDetails delivery = new DeliveryDetails(UUID.randomUUID(), 10, 5, pancakes);

        // exercise
        pancakes.add("Delicious pancake with dark chocolate!");

        // verify
        assertEquals(List.of("Delicious pancake!"), delivery.pancakes());
    }

    @Test
    public void GivenDeliveryDetails_WhenModifyingPancakes_ThenUnsupportedOperationExceptionThrown() {
        // setup
        List<String> pancakes = new ArrayList<>(List.of("Delicious pancake!"));
        DeliveryDetails delivery = new DeliveryDetails(UUID.randomUUID(), 10, 5, pancakes);

        // exercise & verify
        assertThrows(UnsupportedOperationException.class, () -> delivery.pancakes().clear());
    }
}
