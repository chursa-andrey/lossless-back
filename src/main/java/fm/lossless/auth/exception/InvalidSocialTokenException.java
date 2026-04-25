package fm.lossless.auth.exception;

public class InvalidSocialTokenException extends RuntimeException {

    public InvalidSocialTokenException() {
        super("Invalid social auth token");
    }
}
