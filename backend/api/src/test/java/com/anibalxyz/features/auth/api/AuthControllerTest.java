package com.anibalxyz.features.auth.api;

import static com.anibalxyz.features.Helper.capturedJsonAs;
import static com.anibalxyz.features.Helper.stubBodyValidatorFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.anibalxyz.features.auth.api.in.LoginRequest;
import com.anibalxyz.features.auth.api.out.AuthResponse;
import com.anibalxyz.features.auth.application.AuthService;
import com.anibalxyz.features.auth.application.exception.InvalidCredentialsException;
import io.javalin.http.Context;
import io.javalin.validation.ValidationError;
import io.javalin.validation.ValidationException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Auth Controller Tests")
public class AuthControllerTest {
  private static final String VALID_JWT =
"""
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiY\
WRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMn0.KMUFsIDTnFmyG3nMiGM6H9FNFUROf3wh7SmqJp-QV30"
""";

  @Mock private AuthService authService;
  @Mock private Context ctx;

  @InjectMocks private AuthController authController;

  @Nested
  @DisplayName("Success Scenarios")
  class SuccessScenarios {

    @BeforeEach
    void setUp() {
      when(ctx.status(anyInt())).thenReturn(ctx);
    }

    @Test
    @DisplayName("login: given valid credentials, then return JWT")
    public void login_validCredentials_returnJwt() {
      LoginRequest request = new LoginRequest("", "");
      stubBodyValidatorFor(ctx, LoginRequest.class).thenReturn(request);
      when(authService.authenticateUser(request)).thenReturn(VALID_JWT);

      authController.login(ctx);

      verify(ctx).status(200);
      AuthResponse response = capturedJsonAs(ctx, AuthResponse.class);
      assertThat(response.accessToken()).isEqualTo(VALID_JWT);
    }
  }

  @Nested
  @DisplayName("Failure Scenarios")
  class FailureScenarios {

    @Test
    @DisplayName("login: given invalid input, then throw ValidationException")
    public void login_invalidInput_throwsValidationException() {
      stubBodyValidatorFor(ctx, LoginRequest.class)
          .thenThrow(
              new ValidationException(
                  Map.of("email", List.of(new ValidationError<>("Email is required")))));

      assertThatThrownBy(() -> authController.login(ctx)).isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("login: given invalid credentials, then throw InvalidCredentialsException")
    public void login_invalidCredentials_throwsInvalidCredentialsException() {
      LoginRequest request = new LoginRequest("invalid@email.com", "wrongpassword");
      stubBodyValidatorFor(ctx, LoginRequest.class).thenReturn(request);
      when(authService.authenticateUser(request))
          .thenThrow(new InvalidCredentialsException("Invalid credentials"));

      assertThatThrownBy(() -> authController.login(ctx))
          .isInstanceOf(InvalidCredentialsException.class)
          .hasMessage("Invalid credentials");
    }
  }
}
