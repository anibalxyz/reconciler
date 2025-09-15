package com.anibalxyz;

import com.anibalxyz.server.Application;
import com.anibalxyz.server.config.AppConfig;

public class Main {
  public static void main(String[] args) throws Exception {
    Application server = Application.create(AppConfig.loadFromEnv());

    server.start(4000);

    Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
  }
}
