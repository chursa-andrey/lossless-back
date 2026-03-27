package fm.lossless.auth.exception;

public class DefaultRoleNotConfiguredException extends RuntimeException {

    public DefaultRoleNotConfiguredException(String roleCode) {
        super("Default role %s is not configured".formatted(roleCode));
    }
}
