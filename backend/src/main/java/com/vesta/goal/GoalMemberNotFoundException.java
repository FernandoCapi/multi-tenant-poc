package com.vesta.goal;

/** The user is not a member of this goal (404). */
public class GoalMemberNotFoundException extends RuntimeException {

    public GoalMemberNotFoundException() {
        super("User is not a member of the goal");
    }
}
