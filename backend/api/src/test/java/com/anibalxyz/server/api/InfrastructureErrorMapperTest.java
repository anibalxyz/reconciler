package com.anibalxyz.server.api;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

import com.anibalxyz.features.auth.api.exception.AccessDenied;
import com.anibalxyz.shared.UnitTest;
import io.javalin.http.HandlerType;
import io.javalin.router.EndpointNotFound;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

public class InfrastructureErrorMapperTest extends UnitTest {

  static Stream<Arguments> exceptionScenarios() {
    return Stream.of(
        // JsonError
        Arguments.of(new UnrecognizedPropertyException(null, null, null, null, null, null), 400),
        Arguments.of(new StreamReadException(null, ""), 400),
        Arguments.of(mock(MismatchedInputException.class), 400),
        // NotFound
        Arguments.of(new EndpointNotFound(HandlerType.GET, "/some/path"), 404),
        // HttpError
        Arguments.of(new AccessDenied(), 403),
        // Fallback
        Arguments.of(new RuntimeException("Crash!"), 500));
  }

  @ParameterizedTest
  @MethodSource("exceptionScenarios")
  @DisplayName("map: given an exception, then map to correct response")
  void map_exception_mapToCorrectResponse(Exception inputException, int expectedStatus) {
    ErrorResult result = InfrastructureErrorMapper.map(inputException);

    assertThat(result.status()).isEqualTo(expectedStatus);
    assertThat(result.response()).isNotNull();
  }
}
