package com.vesta.goal;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @GetMapping
    public List<GoalSummaryResponse> list(@RequestParam(required = false) GoalStatus status) {
        return goalService.list(status);
    }

    @PostMapping
    public ResponseEntity<GoalDetailResponse> create(@Valid @RequestBody CreateGoalRequest request) {
        GoalDetailResponse created = goalService.create(request);
        return ResponseEntity.created(URI.create("/api/goals/" + created.id())).body(created);
    }

    @GetMapping("/{goalId}")
    public GoalDetailResponse detail(@PathVariable UUID goalId) {
        return goalService.detail(goalId);
    }

    /** Validated inside the service, after the visibility and permission checks. */
    @PatchMapping("/{goalId}")
    public GoalDetailResponse update(@PathVariable UUID goalId, @RequestBody UpdateGoalRequest request) {
        return goalService.update(goalId, request);
    }

    @PostMapping("/{goalId}/complete")
    public GoalDetailResponse complete(@PathVariable UUID goalId) {
        return goalService.complete(goalId);
    }

    @PostMapping("/{goalId}/reopen")
    public GoalDetailResponse reopen(@PathVariable UUID goalId) {
        return goalService.reopen(goalId);
    }

    @PostMapping("/{goalId}/archive")
    public GoalDetailResponse archive(@PathVariable UUID goalId) {
        return goalService.archive(goalId);
    }

    @PostMapping("/{goalId}/unarchive")
    public GoalDetailResponse unarchive(@PathVariable UUID goalId) {
        return goalService.unarchive(goalId);
    }

    @DeleteMapping("/{goalId}")
    public ResponseEntity<Void> delete(@PathVariable UUID goalId) {
        goalService.delete(goalId);
        return ResponseEntity.noContent().build();
    }
}
