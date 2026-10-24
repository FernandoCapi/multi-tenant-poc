package com.vesta.contribution;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReverseContributionRequest(@NotBlank @Size(max = 500) String reason) {
}
