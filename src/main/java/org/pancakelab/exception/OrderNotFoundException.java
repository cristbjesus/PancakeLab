package org.pancakelab.exception;

import java.util.UUID;

public class OrderNotFoundException extends PancakeLabException {
    private final UUID orderId;

    public OrderNotFoundException(UUID orderId) {
        super("Order %s was not found.".formatted(orderId));
        this.orderId = orderId;
    }

    public UUID getOrderId() {
        return orderId;
    }
}
