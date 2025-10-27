package com.anibalxyz.features;

import static com.anibalxyz.features.Constants.Environment.BCRYPT_LOG_ROUNDS;
import static com.anibalxyz.features.Constants.Users.VALID_PASSWORD;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.anibalxyz.features.users.domain.Email;
import com.anibalxyz.features.users.domain.PasswordHash;
import com.anibalxyz.features.users.domain.User;
import com.anibalxyz.features.users.infra.JpaUserRepository;
import com.anibalxyz.features.users.infra.UserEntity;
import com.anibalxyz.persistence.EntityManagerProvider;
import io.javalin.http.Context;
import io.javalin.http.Cookie;
import io.javalin.validation.BodyValidator;
import jakarta.persistence.EntityManager;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.OngoingStubbing;

public class Helper {

  @SuppressWarnings("unchecked")
  public static <T> OngoingStubbing<T> stubBodyValidatorFor(Context ctx, Class<T> clazz) {
    BodyValidator<T> mockValidator = (BodyValidator<T>) mock(BodyValidator.class);
    when(ctx.bodyValidator(clazz)).thenReturn(mockValidator);
    when(mockValidator.check(any(), anyString())).thenReturn(mockValidator);
    return when(mockValidator.get());
  }

  public static <T> T capturedJsonAs(Context ctx, Class<T> clazz) {
    ArgumentCaptor<T> captor = ArgumentCaptor.forClass(clazz);
    verify(ctx).json(captor.capture());
    return captor.getValue();
  }

  public static Cookie capturedCookie(Context ctx) {
    ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
    verify(ctx).cookie(captor.capture());
    return captor.getValue();
  }

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

  public static String capitalize(String s) {
    if (s == null || s.isEmpty()) return s;
    return s.substring(0, 1).toUpperCase() + s.substring(1);
  }

  public static UserEntity persistUser(EntityManager em, String name, String email) {
    em.getTransaction().begin();

    EntityManagerProvider emp = () -> em;
    User saved =
        new JpaUserRepository(emp)
            .save(
                new User(
                    name,
                    new Email(email),
                    PasswordHash.generate(VALID_PASSWORD, BCRYPT_LOG_ROUNDS)));

    em.getTransaction().commit();

    UserEntity entity = em.find(UserEntity.class, saved.getId());
    em.refresh(entity);
    return entity;
  }
}
