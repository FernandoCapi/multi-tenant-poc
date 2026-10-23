package com.vesta.goal;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Bodies are validated inside the service, after the visibility and permission checks. */
@RestController
@RequestMapping("/api/goals/{goalId}/members")
public class GoalMemberController {

    private final GoalMemberService goalMemberService;

    public GoalMemberController(GoalMemberService goalMemberService) {
        this.goalMemberService = goalMemberService;
    }

    @PostMapping
    public GoalDetailResponse add(@PathVariable UUID goalId, @RequestBody NewMemberRequest request) {
        return goalMemberService.add(goalId, request);
    }

    @PatchMapping("/{userId}")
    public GoalDetailResponse update(@PathVariable UUID goalId, @PathVariable UUID userId,
            @RequestBody UpdateMemberRequest request) {
        return goalMemberService.update(goalId, userId, request);
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> remove(@PathVariable UUID goalId, @PathVariable UUID userId) {
        goalMemberService.remove(goalId, userId);
        return ResponseEntity.noContent().build();
    }
}
