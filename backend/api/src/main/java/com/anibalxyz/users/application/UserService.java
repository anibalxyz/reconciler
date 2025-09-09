package com.anibalxyz.users.application;

import com.anibalxyz.users.application.exception.EntityNotFoundException;
import com.anibalxyz.users.application.in.UserUpdatePayload;
import com.anibalxyz.users.domain.Email;
import com.anibalxyz.users.domain.PasswordHash;
import com.anibalxyz.users.domain.User;
import com.anibalxyz.users.domain.UserRepository;
import java.util.List;
import java.util.Optional;

public class UserService {
  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public User createUser(String name, String rawEmail, String rawPassword) {
    Email email = new Email(rawEmail);
    userRepository
        .findByEmail(email)
        .ifPresent(
            user -> {
              throw new IllegalArgumentException("Email already in use. Please use another");
            });
    PasswordHash passwordHash = PasswordHash.generate(rawPassword);
    return userRepository.save(new User(name, email, passwordHash));
  }

  public User updateUser(Integer id, UserUpdatePayload payload) {
    User user =
        userRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
    if (payload.name() != null) {
      user = user.withName(payload.name());
    }
    if (payload.email() != null) {
      Email newEmail = new Email(payload.email());
      userRepository
          .findByEmail(newEmail)
          .ifPresent(
              existingUser -> {
                if (!existingUser.getId().equals(id)) {
                  throw new IllegalArgumentException("Email already in use. Please use another");
                }
              });
      user = user.withEmail(newEmail);
    }
    if (payload.password() != null) {
      user = user.withPasswordHash(PasswordHash.generate(payload.password()));
    }
    return userRepository.save(user);
  }

  public Optional<User> getUserById(int id) {
    return userRepository.findById(id);
  }

  public List<User> getAllUsers() {
    return userRepository.findAll();
  }

  public void deleteUserById(int id) {
    boolean wasDeleted = userRepository.deleteById(id);
    if (!wasDeleted) {
      throw new EntityNotFoundException("User with id " + id + " not found");
    }
  }
}
