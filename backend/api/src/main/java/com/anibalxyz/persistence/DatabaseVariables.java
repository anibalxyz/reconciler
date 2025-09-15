package com.anibalxyz.persistence;

import org.jetbrains.annotations.NotNull;

public class DatabaseVariables {
  public final String url;
  public final String user;
  public final String password;

  private DatabaseVariables(String url, String user, String password) {
    this.url = url;
    this.user = user;
    this.password = password;
  }

  public static DatabaseVariables generate(
      String host, String port, String name, String user, String password) {
    if (host == null || name == null || user == null || password == null) {
      throw new IllegalStateException(
          "Missing required database environment variables (DB_HOST, DB_NAME, DB_USER, DB_PASSWORD)");
    }
    String url = "jdbc:postgresql://" + host + ":" + port + "/" + name;

    return new DatabaseVariables(url, user, password);
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

  @NotNull
  @Override
  public String toString() {
    return "DatabaseVariables[" + "jdbcUrl=" + url + ", " + "user=" + user + ", password=********]";
  }
}
