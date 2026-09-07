package com.anibalxyz.server.config;

import com.anibalxyz.server.exception.ConfigurationException.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: add Hikari configuration from env variables
// TODO: adding a env-identifier to each variable (or set of vars) may be useful
/**
 * A factory for creating application configuration from various sources.
 *
 * <p>This class centralizes the logic for loading configuration data, such as database credentials,
 * JWT settings, and other application-level settings. It can load settings from either system
 * environment variables (standard for containerized environments) or a properties/dotenv file.
 */
public class ConfigurationFactory {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationFactory.class);

  private ConfigurationFactory() {}

  /**
   * Loads configuration from the given command-line arguments.
   *
   * <p>Uses the file passed via {@code --env-file} when present, otherwise the system environment.
   * The flag keeps the {@code env} wording by convention; any properties-format file is accepted,
   * not only dotenv ones.
   *
   * @param args the JVM command-line arguments
   * @return a fully populated {@link ApplicationConfiguration} instance
   */
  public static ApplicationConfiguration load(String[] args) {
    return ArgParser.find("--env-file", args)
        .map(Paths::get)
        .map(ConfigurationFactory::loadFromEnvFile)
        .orElseGet(ConfigurationFactory::loadFromEnv);
  }

  /**
   * Loads configuration from system environment variables. This is the standard method for
   * containerized environments like Docker, where variables are passed directly to the container.
   *
   * @return A new {@link ApplicationConfiguration} instance.
   */
  private static ApplicationConfiguration loadFromEnv() {
    log.info("Loading configuration from the system environment.");
    return ApplicationConfiguration.from(System::getenv);
  }

  /**
   * Loads configuration from a properties/dotenv file from the given {@link Path}.
   *
   * <p>This method is intended for local development and testing, allowing developers to manage
   * configuration variables in a file.
   *
   * @return A new {@link ApplicationConfiguration} instance.
   * @throws UnableToLoadConfigurationFile if the specified file cannot be found or read.
   */
  private static ApplicationConfiguration loadFromEnvFile(Path path) {
    Properties props = new Properties();
    try (InputStream in = Files.newInputStream(path)) {
      props.load(in);
    } catch (IOException e) {
      throw new UnableToLoadConfigurationFile(e);
    }
    log.info("Loading configuration from properties/dotenv file.");
    return ApplicationConfiguration.from(props::getProperty);
  }
}
