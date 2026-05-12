package com.anibalxyz.server.api;

import org.slf4j.event.Level;

public class LogEntry {
  private final Level level;
  private final String message;
  private final Object[] args;

  private LogEntry(Level level, String message, Object... args) {
    this.level = level;
    this.message = message;
    this.args = args;
  }

  public static LogEntry warn(String message, Object... args) {
    return new LogEntry(Level.WARN, message, args);
  }

  public static LogEntry debug(String message, Object... args) {
    return new LogEntry(Level.DEBUG, message, args);
  }

  public Level level() {
    return level;
  }

  public String message() {
    return message;
  }

  public Object[] args() {
    return args;
  }
}
