package com.anibalxyz.shared;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public abstract class UnitTest {
  @BeforeAll
  public static void initializeTestEnvironment() {
    Constants.init();
  }
}
