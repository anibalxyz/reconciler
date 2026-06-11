package com.anibalxyz.server.config.modules.runtime;

import com.anibalxyz.persistence.PersistenceManager;
import com.anibalxyz.server.config.modules.startup.StartupConfig;
import com.anibalxyz.server.context.ContextProvider;
import com.anibalxyz.server.context.RequestContext;
import io.javalin.config.JavalinConfig;
import jakarta.persistence.EntityManager;

/**
 * This class hooks into Javalin's request lifecycle to handle resources that need to be created and
 * torn down for each HTTP request.
 */
public class LifecycleConfig implements StartupConfig {

  private final PersistenceManager persistenceManager;

  public LifecycleConfig(PersistenceManager persistenceManager) {
    this.persistenceManager = persistenceManager;
  }

  @Override
  public void apply(JavalinConfig cfg) {
    setEntityManagerLifecycle(cfg);
    setMDCLifecycle(cfg);
  }

  /**
   * Manage the JPA {@link EntityManager}, ensuring that one is created at the beginning of a
   * request and properly closed at the end, whether the request succeeds or fails.
   */
  private void setEntityManagerLifecycle(JavalinConfig cfg) {
    cfg.routes.before(
        ctx -> {
          ContextProvider.set(ctx);
          EntityManager em = persistenceManager.emf().createEntityManager();
          em.getTransaction().begin();
          ctx.attribute("em", em);
        });
    cfg.routes.after(
        ctx -> {
          EntityManager em = ctx.attribute("em");
          boolean existsEm = em != null && em.isOpen();
          try {
            if (!existsEm || !em.getTransaction().isActive()) {
              return;
            }

            if (ctx.status().getCode() >= 400) {
              em.getTransaction().rollback();
            } else {
              em.getTransaction().commit();
            }

          } finally {
            if (existsEm) em.close();
            ContextProvider.clear();
          }
        });
  }

  private void setMDCLifecycle(JavalinConfig cfg) {
    cfg.routes.before(
        ctx -> {
          String requestId = RequestContext.initialize(ctx);
          ctx.attribute(RequestContext.REQUEST_ID_KEY, requestId);
        });
  }
}
