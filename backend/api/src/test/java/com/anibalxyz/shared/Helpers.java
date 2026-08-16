package com.anibalxyz.shared;

import static com.anibalxyz.shared.Constants.Users.VALID_PASSWORD_STRING;
import static org.mockito.Mockito.*;

import com.anibalxyz.features.users.domain.*;
import com.anibalxyz.features.users.infra.JpaUserRepository;
import com.anibalxyz.features.users.infra.UserEntity;
import com.anibalxyz.persistence.EntityManagerProvider;
import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.validation.Validator;
import jakarta.persistence.EntityManager;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.OngoingStubbing;

/**
 * Utility class providing helper methods for mocking, capturing arguments, and managing the
 * database state during tests.
 */
public class Helpers {

  /**
   * Captures the JSON argument passed to {@link Context#json(Object)} for verification.
   *
   * @param ctx The Javalin {@link Context} mock.
   * @param clazz The class type to cast the captured JSON to.
   * @param <T> The type of the JSON object.
   * @return The captured JSON object.
   */
  public static <T> T capturedJsonAs(Context ctx, Class<T> clazz) {
    ArgumentCaptor<T> captor = ArgumentCaptor.forClass(clazz);
    verify(ctx).json(captor.capture());
    return captor.getValue();
  }

  /**
   * Captures the {@link Cookie} argument passed to {@link Context#cookie(Cookie)} for verification.
   *
   * @param ctx The Javalin {@link Context} mock.
   * @return The captured {@link Cookie} object.
   */
  public static Cookie capturedCookie(Context ctx) {
    ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
    verify(ctx).cookie(captor.capture());
    return captor.getValue();
  }

  @SuppressWarnings("unchecked")
  public static OngoingStubbing<Integer> whenGettingPathParamId(Context ctx) {
    Validator<Integer> mockValidator = (Validator<Integer>) mock(Validator.class);
    when(ctx.pathParamAsClass("id", Integer.class)).thenReturn(mockValidator);
    return when(mockValidator.getOrThrow(any()));
  }

  public static void stubStatusChaining(Context ctx) {
    when(ctx.status(anyInt())).thenReturn(ctx);
  }

  /** Cleans all data from the public schema of the database by truncating all tables. */
  public static void cleanDatabase(EntityManager em) {
    em.getTransaction().begin();
    em.createNativeQuery(
            "DO $$ "
                + "DECLARE stmt text; "
                + "BEGIN "
                + "  SELECT 'TRUNCATE TABLE ' || string_agg(quote_ident(tablename), ', ') || ' RESTART IDENTITY CASCADE' "
                + "  INTO stmt "
                + "  FROM pg_tables "
                + "  WHERE schemaname = 'public'; "
                + "  EXECUTE stmt; "
                + "END $$;")
        .executeUpdate();
    em.getTransaction().commit();
  }

  /**
   * @return The capitalized string, or the original string if null or empty.
   */
  public static String capitalize(String s) {
    if (s == null || s.isEmpty()) return s;
    return s.substring(0, 1).toUpperCase() + s.substring(1);
  }

  public static UserEntity persistUser(EntityManager em, String name, String email) {
    return persistUser(em, name, email, VALID_PASSWORD_STRING);
  }

  public static UserEntity persistUser(
      EntityManager em, String name, String email, String password) {
    em.getTransaction().begin();

    EntityManagerProvider emp = () -> em;
    User saved =
        new JpaUserRepository(emp)
            .save(
                User.create(
                    ResultAsserts.success(Name.of(name)),
                    ResultAsserts.success(Email.of(email)),
                    PasswordHash.of(
                        ResultAsserts.success(Password.of(password)),
                        Constants.APP_ENV.BCRYPT_LOG_ROUNDS())));

    em.getTransaction().commit();

    UserEntity entity = em.find(UserEntity.class, saved.id());
    em.refresh(entity);
    return entity;
  }
}
