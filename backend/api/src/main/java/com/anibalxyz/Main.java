package com.anibalxyz;

import com.anibalxyz.server.Application;
import com.anibalxyz.server.config.environment.ConfigurationFactory;

public class Main {
  public static void main(String[] args) {
    Application server = Application.create(ConfigurationFactory.loadFromEnv());

    Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

    server.start(4000);
  }
}
