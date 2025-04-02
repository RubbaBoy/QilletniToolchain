package dev.qilletni.toolchain.exceptions;

public class QilletniInfoFormatException extends RuntimeException {
    
    public QilletniInfoFormatException() {
    }

    public QilletniInfoFormatException(String message) {
        super(message);
    }

    public QilletniInfoFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
