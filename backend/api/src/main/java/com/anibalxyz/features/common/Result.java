package com.anibalxyz.features.common;

/**
 * Represents the result of an operation that can either succeed or fail.
 *
 * <p>A {@code Result} holds either a value (on success) or an error (on error), but never both.
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
  // TODO: migrate to Success and Error|Failure subclasses API
  // TODO: implement unfold method
  private final V value;
  private final E error;

  private Result(V value, E error) {
    this.value = value;
    this.error = error;
  }

  /** Creates a successful {@code Result} with the given value. */
  public static <V, E> Result<V, E> success(V value) {
    return new Result<>(value, null);
  }

  /** Creates a failed {@code Result} with the given error. */
  public static <V, E> Result<V, E> failure(E error) {
    return new Result<>(null, error);
  }

  public boolean isSuccess() {
    return error == null;
  }

  public boolean isFailure() {
    return error != null;
  }

  /**
   * @return the success value. Ensure the operation was successful before access.
   */
  public V getValue() {
    return value;
  }

  /**
   * @return the failure error. Ensure the operation failed before access.
   */
  public E getError() {
    return error;
  }
}
