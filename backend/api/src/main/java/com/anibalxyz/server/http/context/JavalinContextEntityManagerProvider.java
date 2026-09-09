package com.anibalxyz.server.http.context;

import com.anibalxyz.server.persistence.EntityManagerProvider;
import jakarta.persistence.EntityManager;

/**
 * Bridges Javalin's request context with the persistence layer.
 *
 * <p>Retrieves the request-scoped {@link EntityManager} from {@link ContextProvider} to avoid
 * direct dependencies on Javalin in repositories or services.
 */
public class JavalinContextEntityManagerProvider implements EntityManagerProvider {

  @Override
  public EntityManager get() {
    return ContextProvider.get().attribute("em");
  }
}
