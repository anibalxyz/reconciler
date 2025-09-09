package com.anibalxyz.persistence;

import com.anibalxyz.users.infra.UserEntity;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.cfg.HikariCPSettings;
import org.hibernate.hikaricp.internal.HikariCPConnectionProvider;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.tool.schema.Action;

public class PersistenceManager {
  private final EntityManagerFactory emf;

  public PersistenceManager(DatabaseVariables dbVars) {
    this.emf =
        this.getProperties(dbVars.url(), dbVars.user(), dbVars.password())
            .createEntityManagerFactory();
  }

  public EntityManagerFactory getEntityManagerFactory() {
    return this.emf;
  }

  public void shutdown() {
    if (this.emf != null && this.emf.isOpen()) {
      this.emf.close();
    }
  }

  private HibernatePersistenceConfiguration getProperties(
      String url, String user, String password) {
    // TODO: set values dynamically (from env or configuration file)
    return new HibernatePersistenceConfiguration("reconcilerPU")
        .jdbcUrl(url)
        .jdbcCredentials(user, password)
        .provider(HikariCPConnectionProvider.class.getName())
        .property(HikariCPSettings.HIKARI_MAX_SIZE, "20")
        .property(HikariCPSettings.HIKARI_MIN_IDLE_SIZE, "2")
        .property(HikariCPSettings.HIKARI_ACQUISITION_TIMEOUT, "15000")
        .property(HikariCPSettings.HIKARI_IDLE_TIMEOUT, "300000")
        .schemaToolingAction(Action.VALIDATE)
        .managedClasses(UserEntity.class);
  }
}
