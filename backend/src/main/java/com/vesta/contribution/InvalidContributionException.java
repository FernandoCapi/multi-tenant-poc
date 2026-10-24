package com.vesta.contribution;

/** Business validation of a contribution failed (422). The message is shown to the user. */
public class InvalidContributionException extends RuntimeException {

    public InvalidContributionException(String userMessage) {
        super(userMessage);
    }
}
