package com.anibalxyz;

import com.anibalxyz.server.Application;
import com.anibalxyz.server.config.ApplicationConfiguration;
import com.anibalxyz.server.config.ConfigurationFactory;

public class Main {
  public static void main(String[] args) {
    ApplicationConfiguration config =
        ConfigurationFactory.source(ConfigurationFactory.sourceFromArgs(args)).load();
    Application server = Application.create(config);

    Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));

    server.start(config.httpServer().apiPort());
  }
}
