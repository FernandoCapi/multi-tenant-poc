package com.vesta.goal;

/** Missing, in another family, or not visible to the user: always the same 404 (D-09). */
public class GoalNotFoundException extends RuntimeException {

    public GoalNotFoundException() {
        super("Goal not found or not visible to the current user");
    }
}
