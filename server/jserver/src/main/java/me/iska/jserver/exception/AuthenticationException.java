package me.iska.jserver.exception;

/**
 * An exception specifically for authentication.
 */
public class AuthenticationException extends UserException {
    public AuthenticationException(String message) {
        super(message);
    }
}
