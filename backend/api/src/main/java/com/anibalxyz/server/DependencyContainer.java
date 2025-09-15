package com.anibalxyz.server;

import com.anibalxyz.persistence.EntityManagerProvider;
import com.anibalxyz.users.api.UserController;
import com.anibalxyz.users.application.UserService;
import com.anibalxyz.users.domain.UserRepository;
import com.anibalxyz.users.infra.JpaUserRepository;

// TODO: use an external library if needed or useful
public class DependencyContainer {
  private final UserController userController;

  public DependencyContainer(EntityManagerProvider emProvider) {
    UserRepository userRepository = new JpaUserRepository(emProvider);
    UserService userService = new UserService(userRepository);
    userController = new UserController(userService);
  }

  public UserController getUserController() {
    return userController;
  }
}
