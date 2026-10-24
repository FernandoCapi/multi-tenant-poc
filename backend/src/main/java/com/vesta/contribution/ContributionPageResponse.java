package com.vesta.contribution;

import java.util.List;

/** One page of a goal's contributions, most recent first (D-19). */
public record ContributionPageResponse(
        List<ContributionResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
