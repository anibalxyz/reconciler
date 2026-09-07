package com.anibalxyz.persistence;

import com.anibalxyz.server.config.environment.ConfigGroup;
import com.anibalxyz.server.config.environment.ConfigValueReader;
import java.util.LinkedHashMap;
import java.util.Map;

/** Type-safe representation of database connection variables. */
public class DatabaseVariables implements ConfigGroup {
  private final String url;
  private final String user;
  private final String password;

  private DatabaseVariables(String url, String user, String password) {
    this.url = url;
    this.user = user;
    this.password = password;
  }

  public static DatabaseVariables from(ConfigValueReader reader) {
    String name = reader.read("DB_NAME");
    String user = reader.read("DB_USER");
    String password = reader.read("DB_PASSWORD");
    String port = reader.read("DB_PORT");
    String host = reader.read("DB_HOST");

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

  @Override
  public Map<String, Object> toMap() {
    Map<String, Object> databaseMap = new LinkedHashMap<>();
    databaseMap.put("url", url());
    databaseMap.put("user", user());
    return databaseMap;
  }

  @Override
  public String toString() {
    return toMap().toString();
  }
}
