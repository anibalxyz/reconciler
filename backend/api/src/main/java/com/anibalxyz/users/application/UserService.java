package com.anibalxyz.users.application;

import com.anibalxyz.users.application.exception.EntityNotFoundException;
import com.anibalxyz.users.application.in.UserUpdatePayload;
import com.anibalxyz.users.domain.Email;
import com.anibalxyz.users.domain.PasswordHash;
import com.anibalxyz.users.domain.User;
import com.anibalxyz.users.domain.UserRepository;
import org.hibernate.exception.ConstraintViolationException;

import java.util.List;
import java.util.Optional;

public class UserService {
  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  public User createUser(String name, String rawEmail, String rawPassword) {
    Email email = new Email(rawEmail);
    PasswordHash passwordHash = PasswordHash.generate(rawPassword);
    try {
      return this.userRepository.save(new User(name, email, passwordHash));
    } catch (ConstraintViolationException e) {
      throw new IllegalArgumentException("Email already in use. Please use another");
    }
  }

  public User updateUser(Integer id, UserUpdatePayload payload) {
    User user =
        this.userRepository
            .findById(id)
            .orElseThrow(() -> new EntityNotFoundException("User not found"));
    if (payload.name() != null) {
      user = user.withName(payload.name());
    }
    if (payload.email() != null) {
      user = user.withEmail(new Email(payload.email()));
    }
    if (payload.password() != null) {
      user = user.withPasswordHash(PasswordHash.generate(payload.password()));
    }
    // TODO: This currently throws a 500 error if the email is a duplicate.
    // The ConstraintViolationException is thrown at commit time, too late for a try-catch here.
    // The correct fix is to check if the email is already in use by another user before saving.
    return this.userRepository.save(user);
  }

  public Optional<User> getUserById(int id) {
    return this.userRepository.findById(id);
  }

  public List<User> getAllUsers() {
    return this.userRepository.findAll();
  }

  public void deleteUserById(int id) {
    this.userRepository
        .findById(id)
        .orElseThrow(() -> new EntityNotFoundException("User with id " + id + " not found"));
    this.userRepository.deleteById(id);
  }
}
