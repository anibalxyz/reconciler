package com.anibalxyz.shared;

import static com.anibalxyz.shared.Constants.Users.VALID_USER;
import static com.anibalxyz.shared.Helpers.cleanDatabase;
import static com.anibalxyz.shared.Helpers.createValidJwt;

import com.anibalxyz.server.Application;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import tools.jackson.databind.ObjectMapper;

public abstract class IntegrationTest {
  // Tuesday 10:00
  protected static final ZonedDateTime FIXED_NOW =
      LocalDateTime.of(2026, 4, 21, 10, 0).atZone(ZoneId.of("America/Montevideo"));
  protected static final MutableClock testClock =
      new MutableClock(FIXED_NOW.toInstant(), FIXED_NOW.getZone());
  protected static HttpRequest http;
  protected static Application app;
  protected static String validJwt;
  private static EntityManagerFactory emf;
  protected EntityManager em;

  @BeforeAll
  public static void initializeTestEnvironment() {
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

    validJwt = createValidJwt(app.config().env(), testClock, VALID_USER.id());

    Runtime.getRuntime().addShutdownHook(new Thread(app::stop));
  }

  @BeforeEach
  public void openEntityManager() {
    em = emf.createEntityManager();
    cleanDatabase(em);
  }

  @BeforeEach
  public void resetClock() {
    testClock.resetTo(FIXED_NOW.toInstant());
  }

  @AfterEach
  public void closeEntityManager() {
    if (em.isOpen()) {
      em.close();
    }
  }
}
