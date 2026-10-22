package com.vesta.tenant;

import java.sql.PreparedStatement;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;

import org.hibernate.Session;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Binds the current family to every database transaction, so row-level security applies to all
 * SQL in it, including native queries (ADR-0001).
 * The setting is transaction-local ({@code set_config(..., true)}), so it ends at commit or
 * rollback and never leaks to the next borrower of a pooled connection. Without a bound tenant
 * an empty value is set, which matches no row.
 */
public class TenantAwareJpaTransactionManager extends JpaTransactionManager {

    private static final String SET_TENANT_SQL = "select set_config('app.tenant_id', ?, true)";

    public TenantAwareJpaTransactionManager(EntityManagerFactory entityManagerFactory) {
        super(entityManagerFactory);
    }

    @Override
    protected void doBegin(Object transaction, TransactionDefinition definition) {
        super.doBegin(transaction, definition);
        EntityManagerHolder holder =
                (EntityManagerHolder) TransactionSynchronizationManager.getResource(obtainEntityManagerFactory());
        EntityManager entityManager = holder.getEntityManager();
        String tenant = TenantContext.get().map(UUID::toString).orElse("");
        try {
            entityManager.unwrap(Session.class).doWork(connection -> {
                try (PreparedStatement statement = connection.prepareStatement(SET_TENANT_SQL)) {
                    statement.setString(1, tenant);
                    statement.execute();
                }
            });
        } catch (RuntimeException ex) {
            // Never hand out a transaction whose tenant binding failed
            EntityTransaction entityTransaction = entityManager.getTransaction();
            if (entityTransaction.isActive()) {
                entityTransaction.rollback();
            }
            doCleanupAfterCompletion(transaction);
            throw new CannotCreateTransactionException("Could not bind tenant to the transaction", ex);
        }
    }
}
