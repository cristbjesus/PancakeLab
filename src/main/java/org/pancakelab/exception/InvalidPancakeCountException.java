package org.pancakelab.exception;

public class InvalidPancakeCountException extends PancakeLabException {
    private final int count;

    public InvalidPancakeCountException(int count) {
        super("Pancake count must be positive, but was %d.".formatted(count));
        this.count = count;
    }

    public int getCount() {
        return count;
    }
}
