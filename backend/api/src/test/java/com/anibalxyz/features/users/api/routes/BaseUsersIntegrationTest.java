package com.anibalxyz.features.users.api.routes;

import static com.anibalxyz.shared.Helpers.*;

import com.anibalxyz.core.application.ValidationNotification;
import com.anibalxyz.features.auth.application.JwtService;
import com.anibalxyz.features.users.domain.*;
import com.anibalxyz.features.users.domain.error.EmailAlreadyTakenError;
import com.anibalxyz.features.users.domain.error.UserDomainError;
import com.anibalxyz.features.users.infra.JpaUserRepository;
import com.anibalxyz.server.Application;
import com.anibalxyz.server.api.ErrorMapper;
import com.anibalxyz.server.api.ErrorResult;
import com.anibalxyz.shared.Constants;
import com.anibalxyz.shared.HttpRequest;
import com.anibalxyz.shared.ResultAsserts;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.*;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.*;
import tools.jackson.databind.ObjectMapper;

public abstract class BaseUsersIntegrationTest {
  private static final ZonedDateTime FIXED_NOW =
      LocalDateTime.of(2025, 11, 25, 10, 0).atZone(ZoneId.of("America/Montevideo"));
  private static final Clock testClock = Clock.fixed(FIXED_NOW.toInstant(), FIXED_NOW.getZone());
  protected static HttpRequest http;
  protected static String validJwt;
  private static Application app;
  private static EntityManagerFactory emf;
  protected UserRepository userRepository;
  protected EntityManager em;

  @BeforeAll
  public static void setup() {
    if (app != null) {
      return;
    }
    Constants.init();
    app = Application.create(Constants.APP_CONFIG, testClock);
    app.start(0);

    String baseUrl = app.javalin().jettyServer().server().getURI().toString() + "api";
    emf = app.persistenceManager().emf();
    ObjectMapper objectMapper = new ObjectMapper();

    http = new HttpRequest(objectMapper, new OkHttpClient(), baseUrl);

    var jwtService = new JwtService(app.config().env(), testClock);
    validJwt = jwtService.generateToken(1);

    Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
  }

  protected static ErrorResult errorResultFromAlreadyTakenEmail() {
    ValidationNotification<UserDomainError> correspondentError = new ValidationNotification<>();
    correspondentError.add("email", new EmailAlreadyTakenError());
    return ErrorMapper.map(correspondentError);
  }

  protected static ErrorResult errorResultFromInvalidName(String name) {
    ValidationNotification<UserDomainError> correspondentError = new ValidationNotification<>();
    correspondentError.add("name", ResultAsserts.failure(Name.validate(name)));
    return ErrorMapper.map(correspondentError);
  }

  @BeforeEach
  public void openEntityManager() {
    em = emf.createEntityManager();
    userRepository = new JpaUserRepository(() -> em);
    cleanDatabase(em);
  }

  @AfterEach
  public void closeEntityManager() {
    if (em.isOpen()) {
      em.close();
    }
  }
}
