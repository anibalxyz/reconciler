package com.anibalxyz.features.common;

import java.util.ArrayList;
import java.util.List;

public class Notification<E> {
  protected final List<E> errors = new ArrayList<>();

  public void add(E error) {
    this.errors.add(error);
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  public List<E> getErrors() {
    return List.copyOf(errors);
  }
}
