package com.vesta.goal;

/** The action is not allowed in the current state of the goal (409). The message is shown to the user. */
public class GoalStateConflictException extends RuntimeException {

    public GoalStateConflictException(String userMessage) {
        super(userMessage);
    }
}
