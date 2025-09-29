package com.anibalxyz.users.application;

import com.anibalxyz.users.application.exception.EntityNotFoundException;
import com.anibalxyz.users.application.in.UserUpdatePayload;
import com.anibalxyz.users.domain.Email;
import com.anibalxyz.users.domain.PasswordHash;
import com.anibalxyz.users.domain.User;
import com.anibalxyz.users.domain.UserRepository;
import java.util.List;

public class UserService {
  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public List<User> getAllUsers() {
    return userRepository.findAll();
  }

  public User getUserById(int id) throws EntityNotFoundException {
    return userRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException("User with id " + id + " not found"));
  }

  public User createUser(UserUpdatePayload payload) throws IllegalArgumentException {
    Email email = new Email(payload.email());
    userRepository
        .findByEmail(email)
        .ifPresent(
            user -> {
              throw new IllegalArgumentException("Email already in use. Please use another");
            });
    PasswordHash passwordHash = PasswordHash.generate(payload.password());
    return userRepository.save(new User(payload.name(), email, passwordHash));
  }

  public User updateUserById(Integer id, UserUpdatePayload payload)
      throws IllegalArgumentException, EntityNotFoundException {
    User user =
        userRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
    if (payload.name() != null) {
      user = user.withName(payload.name());
    }
    if (payload.email() != null) {
      Email newEmail = new Email(payload.email());

      if (newEmail.equals(user.getEmail())) {
        return user;
      }

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

  public void deleteUserById(int id) throws EntityNotFoundException {
    boolean wasDeleted = userRepository.deleteById(id);
    if (!wasDeleted) {
      throw new EntityNotFoundException("User with id " + id + " not found");
    }
  }
}
