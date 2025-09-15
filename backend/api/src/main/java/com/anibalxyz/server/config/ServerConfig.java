package com.anibalxyz.server.config;

// NOTE: configs are hardcoded in some way, would be better to make them dynamic through env vars
public interface ServerConfig {
  void apply();
}
