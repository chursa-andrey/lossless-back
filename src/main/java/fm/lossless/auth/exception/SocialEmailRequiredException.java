package fm.lossless.auth.exception;

public class SocialEmailRequiredException extends RuntimeException {

    public SocialEmailRequiredException() {
        super("Verified email is required for social auth");
    }
}
