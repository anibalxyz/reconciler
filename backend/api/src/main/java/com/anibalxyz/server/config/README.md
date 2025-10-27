# Server Config Module

This module contains the configuration classes for the Javalin server, centralizing all startup and runtime settings.

<details>
<summary>Table of Contents</summary>

| Section                             | Description                                                |
|:------------------------------------|:-----------------------------------------------------------|
| [Overview](#overview)               | High-level description of the server configuration module. |
| [Structure](#structure)             | Details on how the configuration module is organized.      |
| [Main Components](#main-components) | Key classes and configurations provided by the module.     |

</details>

## Overview

The `config` module is responsible for managing all aspects of the Javalin server's configuration. It provides a
structured and modular way to apply settings related to the web server itself, environment variables, exception
handling, security middleware, scheduling, and API documentation. By separating these concerns into distinct
configuration classes, the module enhances maintainability and allows for environment-specific adjustments.

## Structure

The `config` module is organized into the following packages:

- `environment/`: Contains classes responsible for loading and providing environment-specific configuration values (
  e.g., from `.env` files or system properties). This ensures that the application behaves correctly across different
  deployment environments (development, test, production).
- `modules/`: Contains modular configuration classes, further divided into `runtime` and `startup` sub-packages. This
  division reflects when the configurations are applied during the application's lifecycle.
    - `startup/`: Configurations applied during the initial setup of the Javalin server, before it starts handling
      requests (e.g., `ServerConfig`, `SwaggerConfig`).
    - `runtime/`: Configurations applied to the running Javalin server instance, often involving request lifecycle hooks
      or scheduled tasks (e.g., `ExceptionsConfig`, `JwtMiddlewareConfig`, `LifecycleConfig`, `SchedulerConfig`).

## Main Components

- **ApplicationConfiguration:** An immutable record that acts as a central container for all application configuration
  objects, including environment-specific settings and database connection details.
- **ConfigurationFactory:** A utility class responsible for creating `ApplicationConfiguration` instances by loading
  settings from various sources, such as system environment variables or `.env` files.
- **StartupConfig and RuntimeConfig:** Abstract base classes that define the contract for configuration modules applied
  during server startup and runtime, respectively. Concrete configuration classes extend these to implement specific
  settings.
- **ExceptionsConfig:** Configures global exception handlers for the Javalin server, mapping application-specific
  exceptions to appropriate HTTP status codes and standardized error responses.
- **JwtMiddlewareConfig:** Applies the JWT authentication middleware to protected routes, enforcing role-based access
  control based on validated JSON Web Tokens.
- **LifecycleConfig:** Manages per-request lifecycle events, particularly for JPA `EntityManager` instances, ensuring
  proper transaction management and resource cleanup for each HTTP request.
- **SchedulerConfig:** Initializes and manages background tasks, such as the periodic cleanup of expired refresh tokens,
  ensuring system maintenance without blocking the main application thread.
- **ServerConfig:** Applies fundamental, one-time configurations to the Javalin server, including virtual threads,
  trailing slash handling, graceful shutdown, default content types, CORS policies, and JSON serialization settings.
- **SwaggerConfig:** Integrates OpenAPI documentation and Swagger UI, providing interactive API documentation and
  exploration capabilities, configurable for different environments.