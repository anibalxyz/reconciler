package com.anibalxyz;

import com.anibalxyz.server.Application;
import com.anibalxyz.server.config.AppConfig;

public class Main {
  public static void main(String[] args) {
    Application server = Application.create(AppConfig.loadFromEnv());

    Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

    server.start(4000);
  }
}
