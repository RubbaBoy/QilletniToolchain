package is.yarr.qilletni.toolchain.exceptions;

public class InvalidVersionException extends RuntimeException {

    public InvalidVersionException() {
    }

    public InvalidVersionException(String message) {
        super(message);
    }

    public InvalidVersionException(String message, Throwable cause) {
        super(message, cause);
    }
}
