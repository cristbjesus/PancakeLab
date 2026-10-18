package org.pancakelab.exception;

public abstract class PancakeLabException extends RuntimeException {
    protected PancakeLabException(String message) {
        super(message);
    }
}
