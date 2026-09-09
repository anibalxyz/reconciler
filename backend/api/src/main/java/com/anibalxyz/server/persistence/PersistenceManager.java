package com.anibalxyz.server.persistence;

import com.anibalxyz.features.auth.infra.RefreshTokenEntity;
import com.anibalxyz.features.users.infra.UserEntity;
import com.anibalxyz.server.config.settings.DatabaseSettings;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.cfg.HikariCPSettings;
import org.hibernate.hikaricp.internal.HikariCPConnectionProvider;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the lifecycle of the JPA {@link EntityManagerFactory}.
 *
 * <p>This class is responsible for initializing the persistence layer. It configures and creates a
 * singleton {@code EntityManagerFactory} using Hibernate and a HikariCP connection pool. It also
 * provides a graceful shutdown mechanism.
 */
public class PersistenceManager {
  private static final Logger log = LoggerFactory.getLogger(PersistenceManager.class);
  private final EntityManagerFactory emf;
  private final DatabaseSettings dbConfig;

  public PersistenceManager(DatabaseSettings dbConfig) {
    this.dbConfig = dbConfig;
    log.info("Initializing database connection pool: {}", dbConfig);
    this.emf = getProperties().createEntityManagerFactory();
    log.info("Database connection pool initialized successfully");
  }

  public EntityManagerFactory emf() {
    return emf;
  }

  /** Closes the {@link EntityManagerFactory} to release all database resources. */
  public void close() {
    if (emf.isOpen()) emf.close();
  }

  /**
   * @return the programmatically configured {@link HibernatePersistenceConfiguration}.
   */
  private HibernatePersistenceConfiguration getProperties() {
    return new HibernatePersistenceConfiguration("reconcilerPU")
        .jdbcUrl(dbConfig.url())
        .jdbcCredentials(dbConfig.user(), dbConfig.password())
        .provider(HikariCPConnectionProvider.class.getName())
        .property(HikariCPSettings.HIKARI_MAX_SIZE, dbConfig.hikari().maxSize())
        .property(HikariCPSettings.HIKARI_MIN_IDLE_SIZE, dbConfig.hikari().minIdleSize())
        .property(
            HikariCPSettings.HIKARI_ACQUISITION_TIMEOUT,
            dbConfig.hikari().acquisitionTimeoutMillis())
        .property(
            HikariCPSettings.HIKARI_VALIDATION_TIMEOUT, dbConfig.hikari().validationTimeoutMillis())
        .property(
            HikariCPSettings.HIKARI_INITIALIZATION_TIMEOUT,
            dbConfig.hikari().initializationTimeoutMillis())
        .property(HikariCPSettings.HIKARI_IDLE_TIMEOUT, dbConfig.hikari().idleTimeoutMillis())
        .schemaToolingAction(Action.VALIDATE)
        .managedClasses(UserEntity.class, RefreshTokenEntity.class);
  }
}
