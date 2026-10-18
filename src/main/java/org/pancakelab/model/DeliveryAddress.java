package org.pancakelab.model;

import org.pancakelab.exception.InvalidRoomException;

public record DeliveryAddress(int building, int room) {
    public DeliveryAddress {
        if (room <= 0) {
            throw new InvalidRoomException(room);
        }
    }
}
