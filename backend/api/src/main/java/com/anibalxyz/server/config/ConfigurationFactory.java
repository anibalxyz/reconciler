package com.anibalxyz.server.config;

import com.anibalxyz.server.config.ConfigurationException.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A factory for creating application configuration from various sources.
 *
 * <p>This class centralizes the logic for loading configuration data, such as database credentials,
 * JWT settings, and other application-level settings. It can load settings from either system
 * environment variables (standard for containerized environments) or a properties/dotenv file.
 */
public class ConfigurationFactory {
  private static final Logger log = LoggerFactory.getLogger(ConfigurationFactory.class);
  private final Function<String, String> source;

  private ConfigurationFactory(Function<String, String> source) {
    this.source = source;
  }

  /**
   * Creates a factory that assembles configuration from the given source.
   *
   * @param source resolves a variable name to its raw value (e.g. {@code System::getenv})
   * @return a factory; call {@link #load()} to assemble the configuration
   */
  public static ConfigurationFactory source(Function<String, String> source) {
    return new ConfigurationFactory(source);
  }

  /**
   * Sources configuration from the given command-line arguments.
   *
   * <p>Uses the file passed via {@code --env-file} when present, otherwise the system environment.
   * The flag keeps the {@code env} wording by convention; any properties-format file is accepted,
   * not only dotenv ones.
   *
   * @param args the JVM command-line arguments
   * @return the selected sourcing method
   */
  public static Function<String, String> sourceFromArgs(String[] args) {
    return ArgParser.find("--env-file", args)
        .map(Paths::get)
        .map(ConfigurationFactory::sourceFromFile)
        .orElseGet(ConfigurationFactory::sourceFromSystem);
  }

  /**
   * Sources from system environment variables. This is the standard method for containerized
   * environments like Docker, where variables are passed directly to the container.
   */
  private static Function<String, String> sourceFromSystem() {
    log.info("Sourcing configuration from the system environment...");
    return System::getenv;
  }

  /**
   * Sources a properties/dotenv file from the given {@link Path}.
   *
   * <p>This method is intended for local development and testing, allowing developers to manage
   * configuration variables in a file.
   *
   * @param path the properties/dotenv file to read
   * @throws UnableToLoadConfigurationFile if the specified file cannot be found or read.
   * @return a sourcing method backed by the file contents
   */
  private static Function<String, String> sourceFromFile(Path path) {
    Properties props = getPropertiesFromPath(path);
    log.info("Sourcing configuration from a properties/dotenv file...");
    return props::getProperty;
  }

  private static Properties getPropertiesFromPath(Path path) {
    Properties props = new Properties();
    try (InputStream in = Files.newInputStream(path)) {
      props.load(in);
    } catch (IOException e) {
      throw new UnableToLoadConfigurationFile(e);
    }
    return props;
  }

  /**
   * Assembles a {@link ApplicationConfiguration} instance from the provided source.
   *
   * @return a fully populated {@link ApplicationConfiguration} instance
   */
  public ApplicationConfiguration load() {
    return ApplicationConfiguration.from(source);
  }
}
