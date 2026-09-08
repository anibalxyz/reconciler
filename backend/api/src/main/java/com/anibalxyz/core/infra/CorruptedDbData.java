package com.anibalxyz.core.infra;

public abstract class CorruptedDbData extends InfrastructureException {
  public CorruptedDbData(String message) {
    super(message);
  }
}
