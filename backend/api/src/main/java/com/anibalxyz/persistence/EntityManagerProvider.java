package com.anibalxyz.persistence;

import jakarta.persistence.EntityManager;

public interface EntityManagerProvider {
  public EntityManager get();
}
