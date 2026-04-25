package fm.lossless.auth.exception;

public class SocialEmailNotVerifiedException extends RuntimeException {

    public SocialEmailNotVerifiedException() {
        super("Social auth email is not verified by provider");
    }
}
