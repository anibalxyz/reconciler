package com.anibalxyz.features.users.infra.exception;

import com.anibalxyz.features.common.infra.exception.CorruptedDbData;

public class CorruptedUserId extends CorruptedDbData {
  public CorruptedUserId(Integer userId) {
    super("Corrupted userId '" + userId + "'.");
  }
}
