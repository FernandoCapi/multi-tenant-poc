package com.vesta.contribution;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Contributions are immutable (D-02): there is deliberately no update or delete endpoint.
 * Bodies are validated inside the service, after the visibility and permission checks.
 */
@RestController
@RequestMapping("/api/goals/{goalId}/contributions")
public class ContributionController {

    private final ContributionService contributionService;

    public ContributionController(ContributionService contributionService) {
        this.contributionService = contributionService;
    }

    @GetMapping
    public ContributionPageResponse list(@PathVariable UUID goalId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return contributionService.list(goalId, page, size);
    }

    @PostMapping
    public ResponseEntity<ContributionResponse> register(@PathVariable UUID goalId,
            @RequestBody RegisterContributionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(contributionService.register(goalId, request));
    }

    @PostMapping("/{contributionId}/reversal")
    public ContributionResponse reverse(@PathVariable UUID goalId, @PathVariable UUID contributionId,
            @RequestBody ReverseContributionRequest request) {
        return contributionService.reverse(goalId, contributionId, request);
    }
}
