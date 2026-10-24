package com.vesta.goal;

/** A goal together with the current user's membership in it. */
public record GoalAccess(Goal goal, GoalMember membership) {

    public boolean isOwner() {
        return membership.isOwner();
    }
}
