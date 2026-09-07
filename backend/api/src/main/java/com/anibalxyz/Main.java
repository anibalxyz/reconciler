package com.anibalxyz;

import com.anibalxyz.server.Application;
import com.anibalxyz.server.config.ApplicationConfiguration;
import com.anibalxyz.server.config.ConfigurationFactory;

public class Main {
  public static void main(String[] args) {
    ApplicationConfiguration config = ConfigurationFactory.load(args);
    Application server = Application.create(config);

    Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

    server.start(config.httpServer().apiPort());
  }
}
