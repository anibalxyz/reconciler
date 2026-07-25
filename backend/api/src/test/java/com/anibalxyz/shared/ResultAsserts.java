package com.anibalxyz.shared;

import com.anibalxyz.core.Result;
import java.util.Arrays;

public final class ResultAsserts {
  public static <V, E> V success(Result<V, E> result) {
    return switch (result) {
      case Result.Success(var value) -> value;
      case Result.Failure(var error) -> {
        throw assertionFailedError(
            "Expected Result.Success but was Result.Failure containing: " + error);
      }
    };
  }

  public static <V, E> E failure(Result<V, E> result) {
    return switch (result) {
      case Result.Failure(var error) -> error;
      case Result.Success(var value) -> {
        throw assertionFailedError(
            "Expected Result.Failure but was Result.Success containing: " + value);
      }
    };
  }

  private static AssertionError assertionFailedError(String message) {
    AssertionError exception = new AssertionError(message);

    int methodsToSkip = 2; // assertSuccess|assertFailure + this method
    StackTraceElement[] originalTrace = exception.getStackTrace();
    StackTraceElement[] cleanedTrace =
        Arrays.copyOfRange(originalTrace, methodsToSkip, originalTrace.length);

    exception.setStackTrace(cleanedTrace);
    return exception;
  }
}
