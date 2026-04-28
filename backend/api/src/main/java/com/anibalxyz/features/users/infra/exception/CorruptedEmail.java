package com.anibalxyz.features.users.infra.exception;

import com.anibalxyz.features.common.infra.exception.CorruptedDbData;

public class CorruptedEmail extends CorruptedDbData {
  public CorruptedEmail(String email, int userId) {
    super("Corrupted email '" + email + "' for user with id " + userId + ".");
  }
}
