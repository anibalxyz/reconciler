package com.anibalxyz.features.common;

/**
 * Represents the result of an operation that can either succeed or fail.
 *
 * <p>A {@code Result} holds either a value (on success) or an error (on failure), but never both.
 * This avoids using exceptions for flow control, allowing callers to handle multiple potential
 * failures without short-circuiting on the first one.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * Result<Email, String> result = Email.of("invalid");
 *
 * if (result.isFailure()) {
 *     errors.add(result.getError());
 * } else {
 *     Email email = result.getValue();
 * }
 * }</pre>
 *
 * @param <V> the type of the success value
 * @param <E> the type of the failure error
 */
public final class Result<V, E> {

  private final V value;
  private final E error;

  private Result(V value, E error) {
    this.value = value;
    this.error = error;
  }

  /**
   * Creates a successful {@code Result} with the given value.
   *
   * @param value the success value
   * @param <V> the type of the success value
   * @param <E> the type of the failure error
   * @return a successful {@code Result}
   */
  public static <V, E> Result<V, E> success(V value) {
    return new Result<>(value, null);
  }

  /**
   * Creates a failed {@code Result} with the given error.
   *
   * @param error the failure error
   * @param <V> the type of the success value
   * @param <E> the type of the failure error
   * @return a failed {@code Result}
   */
  public static <V, E> Result<V, E> failure(E error) {
    return new Result<>(null, error);
  }

  /**
   * Returns {@code true} if this result represents a success.
   *
   * @return {@code true} if successful
   */
  public boolean isSuccess() {
    return error == null;
  }

  /**
   * Returns {@code true} if this result represents a failure.
   *
   * @return {@code true} if failed
   */
  public boolean isFailure() {
    return error != null;
  }

  /**
   * Returns the success value, or {@code null} if this result is a failure.
   *
   * <p>Always check {@link #isSuccess()} before calling this method.
   *
   * @return the success value, or {@code null}
   */
  public V getValue() {
    return value;
  }

  /**
   * Returns the failure error, or {@code null} if this result is a success.
   *
   * <p>Always check {@link #isFailure()} before calling this method.
   *
   * @return the failure error, or {@code null}
   */
  public E getError() {
    return error;
  }
}
