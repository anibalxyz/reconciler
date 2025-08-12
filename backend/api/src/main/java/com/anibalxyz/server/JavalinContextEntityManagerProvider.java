package com.anibalxyz.server;

import com.anibalxyz.persistence.EntityManagerProvider;
import jakarta.persistence.EntityManager;

public class JavalinContextEntityManagerProvider implements EntityManagerProvider {
  @Override
  public EntityManager get() {
    return ContextProvider.get().attribute("em");
  }
}
