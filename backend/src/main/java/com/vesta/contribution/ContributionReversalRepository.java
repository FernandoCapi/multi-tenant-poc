package com.vesta.contribution;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ContributionReversalRepository extends JpaRepository<ContributionReversal, UUID> {

    boolean existsByContributionId(UUID contributionId);

    List<ContributionReversal> findByContributionIdIn(Collection<UUID> contributionIds);
}
