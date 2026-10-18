package org.pancakelab.exception;

import java.util.UUID;

public class PancakeNotFoundException extends PancakeLabException {
    private final UUID pancakeId;

    public PancakeNotFoundException(UUID pancakeId) {
        super("Pancake %s was not found in the order.".formatted(pancakeId));
        this.pancakeId = pancakeId;
    }

    public UUID getPancakeId() {
        return pancakeId;
    }
}
