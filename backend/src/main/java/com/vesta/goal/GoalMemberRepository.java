package com.vesta.goal;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalMemberRepository extends JpaRepository<GoalMember, GoalMemberId> {

    List<GoalMember> findByGoalIdOrderByAddedAt(UUID goalId);

    List<GoalMember> findByUserId(UUID userId);

    long countByGoalIdAndRole(UUID goalId, GoalRole role);

    void deleteByGoalId(UUID goalId);
}
