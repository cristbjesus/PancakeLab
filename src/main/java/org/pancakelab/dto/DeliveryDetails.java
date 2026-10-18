package org.pancakelab.dto;

import java.util.List;
import java.util.UUID;

public record DeliveryDetails(UUID orderId, int building, int room, List<String> pancakes) {
    public DeliveryDetails {
        pancakes = List.copyOf(pancakes);
    }
}
