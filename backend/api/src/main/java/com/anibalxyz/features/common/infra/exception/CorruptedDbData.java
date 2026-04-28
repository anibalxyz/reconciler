package com.anibalxyz.features.common.infra.exception;

public class CorruptedDbData extends RuntimeException {
  public CorruptedDbData(String message) {
    super(message);
  }
}
