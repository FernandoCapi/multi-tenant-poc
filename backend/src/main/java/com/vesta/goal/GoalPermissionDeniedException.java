package com.vesta.goal;

/** The user sees the goal but may not perform the action (403, D-09). */
public class GoalPermissionDeniedException extends RuntimeException {

    public GoalPermissionDeniedException() {
        super("Current user may not perform this action on the goal");
    }
}
