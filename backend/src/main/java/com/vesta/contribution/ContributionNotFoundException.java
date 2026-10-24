package com.vesta.contribution;

/** No such contribution in this goal, or the goal is not visible (404). */
public class ContributionNotFoundException extends RuntimeException {

    public ContributionNotFoundException() {
        super("Contribution not found in the goal");
    }
}
