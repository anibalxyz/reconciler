package com.anibalxyz.shared;

import org.junit.jupiter.api.BeforeAll;

public abstract class UnitTest {
  @BeforeAll
  public static void initializeTestEnvironment() {
    Constants.init();
  }
}
