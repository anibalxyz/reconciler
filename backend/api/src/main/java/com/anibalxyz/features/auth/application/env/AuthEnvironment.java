package com.anibalxyz.features.auth.application.env;

import java.time.Duration;

public interface AuthEnvironment {
  Duration JWT_REFRESH_EXPIRATION_TIME_DAYS();
}
