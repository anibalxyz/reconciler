package com.anibalxyz;

import com.anibalxyz.server.Application;
import com.anibalxyz.server.config.environment.ApplicationConfiguration;
import com.anibalxyz.server.config.environment.ConfigurationFactory;

public class Main {
  public static void main(String[] args) {
    ApplicationConfiguration config = ConfigurationFactory.loadFromEnv();
    Application server = Application.create(config);

    Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

    server.start(config.env().API_PORT());
  }
}
