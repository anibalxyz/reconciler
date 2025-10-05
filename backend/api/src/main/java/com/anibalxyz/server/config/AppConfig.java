package com.anibalxyz.server.config;

import com.anibalxyz.persistence.DatabaseVariables;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
  // Fixed default port in PostgreSQL container
  private static final String DEFAULT_DB_PORT = "5432";
  // Used when running locally because container is mapped to localhost
  private static final String DEFAULT_LOCAL_HOST = "localhost";
  private final EnvVarSet env;
  private final DatabaseVariables database;

  private AppConfig(EnvVarSet env, DatabaseVariables database) {
    this.env = env;
    this.database = database;
  }

  public static AppConfig loadForTest() {
    if (System.getenv("APP_ENV") != null) {
      return loadFromEnv();
    }
    return loadFromEnvFile("test");
  }

  // TODO: add Hikari configuration from env variables or env file
  public static AppConfig loadFromEnv() {
    String appEnv = System.getenv("APP_ENV");
    String host = System.getenv("DB_HOST");
    String name = System.getenv("DB_NAME");
    String user = System.getenv("DB_USER");
    String password = System.getenv("DB_PASSWORD");

    int bcryptLogRounds = Integer.parseInt(System.getenv("BCRYPT_LOG_ROUNDS"));
    EnvVarSet env = new EnvVarSet(appEnv, bcryptLogRounds);

    return new AppConfig(
        env, DatabaseVariables.generate(host, DEFAULT_DB_PORT, name, user, password));
  }

  public static AppConfig loadFromEnvFile(String appEnv) {
    Properties props = new Properties();
    try (InputStream in = new FileInputStream("../.env." + appEnv)) {
      props.load(in);
    } catch (IOException e) {
      throw new IllegalStateException("Could not load .env file for configuration", e);
    }
    String port = props.getProperty("DB_PORT");
    String name = props.getProperty("DB_NAME");
    String user = props.getProperty("DB_USER");
    String password = props.getProperty("DB_PASSWORD");

    int bcryptLogRounds = Integer.parseInt(props.getProperty("BCRYPT_LOG_ROUNDS"));
    EnvVarSet env = new EnvVarSet(appEnv, bcryptLogRounds);

    return new AppConfig(
        env, DatabaseVariables.generate(DEFAULT_LOCAL_HOST, port, name, user, password));
  }

  public EnvVarSet env() {
    return env;
  }

  public DatabaseVariables database() {
    return database;
  }
}
