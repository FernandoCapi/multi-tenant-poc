package com.vesta.user;

import java.util.UUID;

import com.vesta.family.Family;

public record MeResponse(UUID id, String email, String displayName, FamilySummary family) {

    static MeResponse of(User user, Family family) {
        return new MeResponse(user.getId(), user.getEmail(), user.getDisplayName(),
                new FamilySummary(family.getId(), family.getName()));
    }

    public record FamilySummary(UUID id, String name) {
    }
}
