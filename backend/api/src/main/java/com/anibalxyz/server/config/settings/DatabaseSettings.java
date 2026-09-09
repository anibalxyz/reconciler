package com.anibalxyz.server.config.settings;

import java.util.LinkedHashMap;
import java.util.Map;

/** Type-safe representation of database connection variables. */
public class DatabaseSettings implements Settings {
  private final String url;
  private final String user;
  private final String password;
  private final HikariSettings hikari;

  private DatabaseSettings(String url, String user, String password, HikariSettings hikari) {
    this.url = url;
    this.user = user;
    this.password = password;
    this.hikari = hikari;
  }

  public static DatabaseSettings from(SettingsReader reader) {
    String name = reader.read("DB_NAME");
    String user = reader.read("DB_USER");
    String password = reader.read("DB_PASSWORD");
    String port = reader.read("DB_PORT");
    String host = reader.read("DB_HOST");

    String url = "jdbc:postgresql://" + host + ":" + port + "/" + name;
    HikariSettings hikari = HikariSettings.from(reader);

    return new DatabaseSettings(url, user, password, hikari);
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

  public HikariSettings hikari() {
    return hikari;
  }

  @Override
  public Map<String, Object> toMap() {
    Map<String, Object> databaseMap = new LinkedHashMap<>();
    databaseMap.put("url", url());
    databaseMap.put("user", user());
    databaseMap.put("hikari", hikari().toMap());
    return databaseMap;
  }

  @Override
  public String toString() {
    return toMap().toString();
  }
}
