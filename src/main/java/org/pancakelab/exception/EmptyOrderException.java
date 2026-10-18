package org.pancakelab.exception;

import java.util.UUID;

public class EmptyOrderException extends PancakeLabException {
    private final UUID orderId;

    public EmptyOrderException(UUID orderId) {
        super("Order %s has no pancakes.".formatted(orderId));
        this.orderId = orderId;
    }

    public UUID getOrderId() {
        return orderId;
    }
}
