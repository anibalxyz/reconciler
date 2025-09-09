package com.anibalxyz.persistence;

import jakarta.persistence.EntityManager;

public interface EntityManagerProvider {
  EntityManager get();
}
