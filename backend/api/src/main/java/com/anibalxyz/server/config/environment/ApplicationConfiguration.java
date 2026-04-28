package com.anibalxyz.server.config.environment;

import com.anibalxyz.persistence.DatabaseVariables;

/** Simple wrapper for configuration properties that are loaded at startup. */
public record ApplicationConfiguration(AppEnvironmentSource env, DatabaseVariables database) {}
