package org.pancakelab.exception;

public class InvalidRoomException extends PancakeLabException {
    private final int room;

    public InvalidRoomException(int room) {
        super("Room number must be positive, but was %d.".formatted(room));
        this.room = room;
    }

    public int getRoom() {
        return room;
    }
}
