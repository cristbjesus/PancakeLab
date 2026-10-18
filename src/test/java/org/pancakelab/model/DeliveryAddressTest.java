package org.pancakelab.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.pancakelab.exception.InvalidRoomException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DeliveryAddressTest {
    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    public void GivenNonPositiveRoom_WhenCreatingDeliveryAddress_ThenInvalidRoomExceptionThrown(int room) {
        // exercise
        InvalidRoomException exception =
                assertThrows(InvalidRoomException.class, () -> new DeliveryAddress(10, room));

        // verify
        assertEquals(room, exception.getRoom());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, Integer.MAX_VALUE})
    public void GivenPositiveRoom_WhenCreatingDeliveryAddress_ThenAddressCreated(int room) {
        // exercise
        DeliveryAddress address = new DeliveryAddress(10, room);

        // verify
        assertEquals(room, address.room());
    }
}
