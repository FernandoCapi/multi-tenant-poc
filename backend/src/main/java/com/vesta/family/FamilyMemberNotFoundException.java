package com.vesta.family;

/** The user does not exist in the current family; users of other families are never revealed (CA-03.4). */
public class FamilyMemberNotFoundException extends RuntimeException {

    public FamilyMemberNotFoundException() {
        super("User not found in the current family");
    }
}
