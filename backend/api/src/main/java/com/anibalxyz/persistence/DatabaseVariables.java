package com.anibalxyz.persistence;

import com.anibalxyz.server.exception.ConfigurationException;
import org.jetbrains.annotations.NotNull;

/** Type-safe representation of database connection variables. */
public class DatabaseVariables {
  public final String url;
  public final String user;
  public final String password;

  private DatabaseVariables(String url, String user, String password) {
    this.url = url;
    this.user = user;
    this.password = password;
  }

  /**
   * @throws ConfigurationException.MissingProperty if any of the required variables are missing.
   */
  public static DatabaseVariables generate(
      String host, String port, String name, String user, String password) {
    validate(host, port, name, user, password);

    String url = "jdbc:postgresql://" + host + ":" + port + "/" + name;

    return new DatabaseVariables(url, user, password);
  }

  private static void validate(
      String host, String port, String name, String user, String password) {
    if (host == null) throw new ConfigurationException.MissingProperty("host");
    if (port == null) throw new ConfigurationException.MissingProperty("port");
    if (name == null) throw new ConfigurationException.MissingProperty("name");
    if (user == null) throw new ConfigurationException.MissingProperty("user");
    if (password == null) throw new ConfigurationException.MissingProperty("password");
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

  /**
   * @return The string representation of the object, masking the password for security.
   */
  @NotNull
  @Override
  public String toString() {
    return "DatabaseVariables[" + "jdbcUrl=" + url + ", " + "user=" + user + ", password=********]";
  }
}
