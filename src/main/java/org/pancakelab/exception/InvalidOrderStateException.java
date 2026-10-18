package org.pancakelab.exception;

import java.util.UUID;

public class InvalidOrderStateException extends PancakeLabException {
    private final UUID orderId;
    private final String status;

    public InvalidOrderStateException(UUID orderId, String status) {
        super("Order %s is %s, which does not allow this operation.".formatted(orderId, status));
        this.orderId = orderId;
        this.status = status;
    }

    public UUID getOrderId() {
        return orderId;
    }

    public String getStatus() {
        return status;
    }
}
