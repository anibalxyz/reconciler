package com.anibalxyz.features.users.infra.exception;

import com.anibalxyz.features.common.infra.exception.CorruptedDbData;

public class CorruptedName extends CorruptedDbData {
  public CorruptedName(String name, int userId) {
    super("Corrupted name '" + name + "' for user with id " + userId + ".");
  }
}
