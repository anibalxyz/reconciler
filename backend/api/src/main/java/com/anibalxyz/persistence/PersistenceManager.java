package com.anibalxyz.persistence;

import com.anibalxyz.users.infra.UserEntity;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.cfg.HikariCPSettings;
import org.hibernate.hikaricp.internal.HikariCPConnectionProvider;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersistenceManager {
  private static final Logger log = LoggerFactory.getLogger(PersistenceManager.class);
  private final EntityManagerFactory emf;
  private final DatabaseVariables dbConfig;

  public PersistenceManager(DatabaseVariables dbConfig) {
    this.dbConfig = dbConfig;
    emf = getProperties().createEntityManagerFactory();
  }

  public EntityManagerFactory emf() {
    return emf;
  }

  public void shutdown() {
    if (emf != null && emf.isOpen()) {
      emf.close();
    }
  }

  private HibernatePersistenceConfiguration getProperties() {
    // TODO: set values dynamically (from env or configuration file)
    return new HibernatePersistenceConfiguration("reconcilerPU")
        .jdbcUrl(dbConfig.url())
        .jdbcCredentials(dbConfig.user(), dbConfig.password())
        .provider(HikariCPConnectionProvider.class.getName())
        .property(HikariCPSettings.HIKARI_MAX_SIZE, "20")
        .property(HikariCPSettings.HIKARI_MIN_IDLE_SIZE, "2")
        .property(HikariCPSettings.HIKARI_ACQUISITION_TIMEOUT, "15000")
        .property(HikariCPSettings.HIKARI_IDLE_TIMEOUT, "300000")
        .schemaToolingAction(Action.VALIDATE)
        .managedClasses(UserEntity.class);
  }
}
