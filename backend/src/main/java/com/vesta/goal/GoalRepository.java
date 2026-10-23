package com.vesta.goal;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GoalRepository extends JpaRepository<Goal, UUID> {

    /** Goals the user is linked to (INV-03), in the given statuses. */
    @Query("""
            select g from Goal g
             where g.status in :statuses
               and exists (select 1 from GoalMember m where m.goalId = g.id and m.userId = :userId)
             order by g.createdAt, g.title
            """)
    List<Goal> findVisible(@Param("userId") UUID userId, @Param("statuses") Collection<GoalStatus> statuses);

    /** Locks the goal row so membership changes of the same goal are serialized (INV-04). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from Goal g where g.id = :id")
    Optional<Goal> findByIdForUpdate(@Param("id") UUID id);
}
