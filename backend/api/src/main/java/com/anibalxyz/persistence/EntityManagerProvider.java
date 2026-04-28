package com.anibalxyz.persistence;

import jakarta.persistence.EntityManager;

/** Provides a request-scoped {@link EntityManager}. */
public interface EntityManagerProvider {
  /**
   * @return the {@link EntityManager} bound to the current thread or request scope.
   */
  EntityManager get();
}
