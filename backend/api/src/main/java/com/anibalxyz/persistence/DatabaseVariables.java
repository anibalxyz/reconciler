package com.anibalxyz.persistence;

public final class DatabaseVariables {
  private final String url;
  private final String user;
  private final String password;

  private DatabaseVariables(String url, String user, String password) {
    this.url = url;
    this.user = user;
    this.password = password;
  }

  public static DatabaseVariables fromEnv() {
    String host = System.getenv("DB_HOST");
    String name = System.getenv("DB_NAME");
    String user = System.getenv("DB_USER");
    String password = System.getenv("DB_PASSWORD");

    if (host == null || name == null || user == null || password == null) {
      throw new IllegalStateException(
          "Missing required database environment variables (DB_HOST, DB_NAME, DB_USER, DB_PASSWORD)");
    }

    String jdbcUrl = "jdbc:postgresql://" + host + ":5432/" + name;
    return new DatabaseVariables(jdbcUrl, user, password);
  }

  public String url() {
    return url;
  }

  public String user() {
    return user;
  }

  public String password() {
    return password;
  }

  @Override
  public String toString() {
    return "DatabaseVariables[" + "jdbcUrl=" + url + ", " + "user=" + user + ", password=********]";
  }
}
