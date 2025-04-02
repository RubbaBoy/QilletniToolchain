package dev.qilletni.toolchain.exceptions;

public class DependencyNotMetException extends RuntimeException {

    public DependencyNotMetException() {
        super();
    }

    public DependencyNotMetException(String message) {
        super(message);
    }

    public DependencyNotMetException(String message, Throwable cause) {
        super(message, cause);
    }
}
