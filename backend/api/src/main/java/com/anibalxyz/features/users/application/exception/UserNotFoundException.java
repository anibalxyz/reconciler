package com.anibalxyz.features.users.application.exception;

import com.anibalxyz.features.common.application.exception.ResourceNotFoundException;

/**
 * Exception thrown when a user cannot be found by the given identifier.
 *
 * <p>Extends {@link ResourceNotFoundException} and maps to an HTTP 404 Not Found response.
 */
public class UserNotFoundException extends ResourceNotFoundException {

  /**
   * Constructs a new {@code UserNotFoundException} for the given user ID.
   *
   * @param id the ID of the user that could not be found
   */
  public UserNotFoundException(int id) {
    super("User with id " + id + " not found");
  }

  /**
   * Constructs a new {@code UserNotFoundException} for the given email address.
   *
   * @param email the email of the user that could not be found
   */
  public UserNotFoundException(String email) {
    super("User with email " + email + " not found");
  }
}
