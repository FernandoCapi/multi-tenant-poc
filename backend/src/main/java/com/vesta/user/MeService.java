package com.vesta.user;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vesta.family.Family;
import com.vesta.family.FamilyRepository;

@Service
public class MeService {

    private final UserRepository userRepository;
    private final FamilyRepository familyRepository;

    public MeService(UserRepository userRepository, FamilyRepository familyRepository) {
        this.userRepository = userRepository;
        this.familyRepository = familyRepository;
    }

    /** Both lookups run under the tenant of the token, so a user of another family is never found. */
    @Transactional(readOnly = true)
    public MeResponse describe(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(UnknownAuthenticatedUserException::new);
        Family family = familyRepository.findById(user.getFamilyId())
                .orElseThrow(UnknownAuthenticatedUserException::new);
        return MeResponse.of(user, family);
    }
}
