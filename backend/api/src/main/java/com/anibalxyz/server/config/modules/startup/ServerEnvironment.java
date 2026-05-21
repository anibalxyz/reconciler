package com.anibalxyz.server.config.modules.startup;

import com.anibalxyz.server.config.AppEnv;

public interface ServerEnvironment {
  AppEnv APP_ENV();

  String API_URL();

  String SERVER_URL();

  String[] CORS_ALLOWED_ORIGINS();

  String CONTACT_EMAIL();

  String API_PUBLIC_URL();
}
