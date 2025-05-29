package fr.mineria.azauth.demo;

import fr.mineria.azauth.AzAuth;
import lombok.Getter;

@Getter
public class AuthResult {

    private final AuthStatus status;
    private final AuthType type;
    private final AzAuth.User user;
    private final String errorMessage;

    public AuthResult(AuthStatus status, AuthType type, AzAuth.User user, String errorMessage) {
        this.status = status;
        this.type = type;
        this.user = user;
        this.errorMessage = errorMessage;
    }

    public static AuthResult success(AuthType type, AzAuth.User user) {
        return new AuthResult(AuthStatus.SUCCESS, type, user, null);
    }

    public static AuthResult failure(AuthType type, String errorMessage) {
        return new AuthResult(AuthStatus.FAIL, type, null, errorMessage);
    }
}