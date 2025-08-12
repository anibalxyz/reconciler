package com.anibalxyz.persistence;

import com.anibalxyz.users.infra.UserEntity;
import jakarta.persistence.EntityManagerFactory;
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
    return new HibernatePersistenceConfiguration("reconcilerPU")
        .jdbcUrl(url)
        .jdbcCredentials(user, password)
        .schemaToolingAction(Action.VALIDATE)
        .managedClasses(UserEntity.class);
  }
}
