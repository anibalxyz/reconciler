package com.anibalxyz.shared;

import org.flywaydb.core.Flyway;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class TestDb {
  private static PostgreSQLContainer<?> postgres;
  private static boolean initialized;

  public static void init() {
    if (initialized) return;

    postgres =
        new PostgreSQLContainer<>(DockerImageName.parse("postgres:17-alpine"))
            .withDatabaseName("testdb")
            .withUsername("testuser")
            .withPassword("testpassword")
            // requires "testcontainers.reuse.enable=true" in "~/.testcontainers.properties"
            .withReuse(true);
    postgres.start();

    var flyway =
        Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .locations("classpath:db/migrations")
            .cleanDisabled(false)
            .load();

    flyway.clean();
    flyway.migrate();

    initialized = true;
  }

  public static String host() {
    return postgres.getHost();
  }

  public static int port() {
    return postgres.getMappedPort(5432);
  }

  public static String dbName() {
    return postgres.getDatabaseName();
  }

  public static String user() {
    return postgres.getUsername();
  }

  public static String password() {
    return postgres.getPassword();
  }
}
