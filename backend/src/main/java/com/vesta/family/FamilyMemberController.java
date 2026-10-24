package com.vesta.family;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.vesta.user.UserRepository;

/** Members of the token's family, to choose whom to add to a goal (D-01). */
@RestController
public class FamilyMemberController {

    private final UserRepository userRepository;

    public FamilyMemberController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/api/family/members")
    public List<FamilyMemberResponse> members() {
        return userRepository.findAll(Sort.by("displayName")).stream()
                .map(user -> new FamilyMemberResponse(user.getId(), user.getDisplayName()))
                .toList();
    }

    public record FamilyMemberResponse(UUID id, String displayName) {
    }
}
