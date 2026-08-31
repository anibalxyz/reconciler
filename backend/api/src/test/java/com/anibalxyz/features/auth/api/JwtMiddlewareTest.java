package com.anibalxyz.features.auth.api;

import static com.anibalxyz.features.auth.api.JwtMiddleware.AUTHORIZATION_HEADER;
import static com.anibalxyz.features.auth.api.JwtMiddleware.BEARER_PREFIX;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.anibalxyz.core.Result;
import com.anibalxyz.core.application.exception.FailureSignal;
import com.anibalxyz.features.auth.application.JwtService;
import com.anibalxyz.features.auth.application.JwtService.JwtValidationError;
import com.anibalxyz.features.common.api.Role;
import com.anibalxyz.server.context.RequestContext;
import com.anibalxyz.shared.UnitTest;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.UnauthorizedResponse;
import io.javalin.security.RouteRole;
import io.jsonwebtoken.Claims;
import java.util.Collections;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.MDC;

@DisplayName("Tests for JwtMiddleware")
class JwtMiddlewareTest extends UnitTest {

  @Mock private JwtService jwtService;
  @Mock private Context ctx;

  @InjectMocks private JwtMiddleware jwtMiddleware;

  @AfterEach
  void clear() {
    RequestContext.clear();
  }

  @Nested
  @DisplayName("execute()")
  class Execute {

    @Test
    @DisplayName("given empty route roles, then skip JWT validation")
    void givenEmptyRouteRoles_thenSkipValidation() {
      when(ctx.routeRoles()).thenReturn(Collections.emptySet());

      jwtMiddleware.execute(ctx);

      verify(ctx, never()).header(AUTHORIZATION_HEADER);
    }

    @Test
    @DisplayName("given GUEST role, then skip JWT validation")
    void givenGuestRole_thenSkipValidation() {
      when(ctx.routeRoles()).thenReturn(Set.of(Role.GUEST));

      jwtMiddleware.execute(ctx);

      verify(ctx, never()).header(AUTHORIZATION_HEADER);
    }

    @Test
    @DisplayName("given AUTHENTICATED role and valid token, then pass authorization")
    void givenAuthenticatedRoleAndValidToken_thenPass() {
      when(ctx.routeRoles()).thenReturn(Set.of(Role.AUTHENTICATED));
      when(ctx.header(AUTHORIZATION_HEADER)).thenReturn("Bearer valid-token");

      Claims claims = mock(Claims.class);
      when(claims.getSubject()).thenReturn("42");
      when(jwtService.validateToken("valid-token")).thenReturn(Result.success(claims));

      jwtMiddleware.execute(ctx);

      verify(ctx).attribute(JwtMiddleware.JWT_USER_ID, 42);
      assertThat(MDC.get(RequestContext.USER_ID_KEY)).isEqualTo("42");
    }

    @Test
    @DisplayName("given permitted role without AUTHENTICATED, then throw ForbiddenResponse")
    void givenUnmatchedRole_thenThrowForbiddenResponse() {
      RouteRole customRole = mock(RouteRole.class);
      when(ctx.routeRoles()).thenReturn(Set.of(customRole));
      when(ctx.header(AUTHORIZATION_HEADER)).thenReturn(BEARER_PREFIX + "valid-token");

      Claims claims = mock(Claims.class);
      when(claims.getSubject()).thenReturn("42");
      when(jwtService.validateToken("valid-token")).thenReturn(Result.success(claims));

      assertThatThrownBy(() -> jwtMiddleware.execute(ctx))
          .isInstanceOf(ForbiddenResponse.class)
          .hasMessage("Access denied");
    }
  }

  @Nested
  @DisplayName("validateJwt()")
  class ValidateJwt {

    @Test
    @DisplayName("given null header, then throw UnauthorizedResponse")
    void givenNullHeader_thenThrowUnauthorizedResponse() {
      when(ctx.header(AUTHORIZATION_HEADER)).thenReturn(null);

      assertThatThrownBy(() -> jwtMiddleware.validateJwt(ctx))
          .isInstanceOf(UnauthorizedResponse.class)
          .hasMessage("Missing or invalid Authorization header");
    }

    @Test
    @DisplayName("given header without Bearer prefix, then throw UnauthorizedResponse")
    void givenHeaderWithoutBearer_thenThrowUnauthorizedResponse() {
      when(ctx.header(AUTHORIZATION_HEADER)).thenReturn("A" + BEARER_PREFIX + "xyz123");

      assertThatThrownBy(() -> jwtMiddleware.validateJwt(ctx))
          .isInstanceOf(UnauthorizedResponse.class)
          .hasMessage("Missing or invalid Authorization header");
    }

    @Test
    @DisplayName("given valid token, then set RequestContext and context attribute")
    void givenValidToken_thenSetRequestContextAndAttribute() {
      when(ctx.header(AUTHORIZATION_HEADER)).thenReturn(BEARER_PREFIX + "abc.def.ghi");

      Claims claims = mock(Claims.class);
      when(claims.getSubject()).thenReturn("100");
      when(jwtService.validateToken("abc.def.ghi")).thenReturn(Result.success(claims));

      jwtMiddleware.validateJwt(ctx);

      verify(ctx).attribute(JwtMiddleware.JWT_USER_ID, 100);
      assertThat(MDC.get(RequestContext.USER_ID_KEY)).isEqualTo("100");
    }

    @Test
    @DisplayName("given invalid token, then throw FailureSignal")
    void givenInvalidToken_thenThrowFailureSignal() {
      when(ctx.header(AUTHORIZATION_HEADER)).thenReturn(BEARER_PREFIX + "invalid-token");
      when(jwtService.validateToken("invalid-token"))
          .thenReturn(Result.failure(new JwtValidationError.Invalid()));

      assertThatThrownBy(() -> jwtMiddleware.validateJwt(ctx)).isInstanceOf(FailureSignal.class);
    }
  }
}
