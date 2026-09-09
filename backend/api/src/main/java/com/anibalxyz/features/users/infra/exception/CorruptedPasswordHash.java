package com.anibalxyz.features.users.infra.exception;

import com.anibalxyz.core.infra.CorruptedDbData;

public class CorruptedPasswordHash extends CorruptedDbData {
  public CorruptedPasswordHash(String passwordHash, int userId) {
    super("Corrupted passwordHash '" + passwordHash + "' for user with id " + userId + ".");
  }
}
