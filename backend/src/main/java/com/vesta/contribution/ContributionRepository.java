package com.vesta.contribution;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContributionRepository extends JpaRepository<Contribution, UUID> {

    Page<Contribution> findByGoalId(UUID goalId, Pageable pageable);

    Optional<Contribution> findByIdAndGoalId(UUID id, UUID goalId);
}
