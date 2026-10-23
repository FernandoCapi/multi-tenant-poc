package com.vesta.user;

/** The token is valid but its user is not visible in the token's family. */
public class UnknownAuthenticatedUserException extends RuntimeException {

    public UnknownAuthenticatedUserException() {
        super("Authenticated user not found in the bound family");
    }
}
