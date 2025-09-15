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
  private final String env;
  private final DatabaseVariables database;

  private AppConfig(String env, DatabaseVariables database) {
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
    String env = System.getenv("APP_ENV");
    String host = System.getenv("DB_HOST");
    String name = System.getenv("DB_NAME");
    String user = System.getenv("DB_USER");
    String password = System.getenv("DB_PASSWORD");

    return new AppConfig(
        env, DatabaseVariables.generate(host, DEFAULT_DB_PORT, name, user, password));
  }

  public static AppConfig loadFromEnvFile(String env) {
    Properties props = new Properties();
    try (InputStream in = new FileInputStream("../.env." + env)) {
      props.load(in);
    } catch (IOException e) {
      throw new IllegalStateException("Could not load .env file for configuration", e);
    }
    String port = props.getProperty("DB_PORT");
    String name = props.getProperty("DB_NAME");
    String user = props.getProperty("DB_USER");
    String password = props.getProperty("DB_PASSWORD");
    return new AppConfig(
        env, DatabaseVariables.generate(DEFAULT_LOCAL_HOST, port, name, user, password));
  }

  public String env() {
    return env;
  }

  public DatabaseVariables database() {
    return database;
  }
}
