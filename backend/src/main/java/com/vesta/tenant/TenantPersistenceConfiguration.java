package com.vesta.tenant;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.cfg.MultiTenancySettings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.boot.transaction.autoconfigure.TransactionManagerCustomizers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class TenantPersistenceConfiguration {

    @Bean
    HibernatePropertiesCustomizer tenantIdentifierResolverCustomizer() {
        return properties -> properties.put(
                MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, new TenantIdentifierResolver());
    }

    /** Replaces Boot's auto-configured JpaTransactionManager. */
    @Bean
    TenantAwareJpaTransactionManager transactionManager(
            EntityManagerFactory entityManagerFactory,
            ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {
        TenantAwareJpaTransactionManager transactionManager =
                new TenantAwareJpaTransactionManager(entityManagerFactory);
        transactionManagerCustomizers.ifAvailable(customizers -> customizers.customize(transactionManager));
        return transactionManager;
    }
}
