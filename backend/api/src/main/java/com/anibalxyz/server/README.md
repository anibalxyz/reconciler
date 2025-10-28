# Server Module

This module is the heart of the application, responsible for initializing and wiring together all the major components
and orchestrating the application startup.

<details>
<summary>Table of Contents</summary>

| Section                             | Description                                                         |
|:------------------------------------|:--------------------------------------------------------------------|
| [Overview](#overview)               | High-level description of the server module's purpose.              |
| [Structure](#structure)             | Details on how the server module is organized.                      |
| [Main Classes](#main-classes)       | Key classes involved in the server setup and application lifecycle. |
| [Interactions](#interactions)       | How the server module interacts with other core components.         |
| [Design Patterns](#design-patterns) | Key architectural and design patterns applied within the module.    |

</details>

## Overview

The `server` module serves as the **Composition Root** of the entire backend application. Its primary responsibility is
to assemble and configure all the pieces of the application together – from the web framework (Javalin) and persistence
layer (Hibernate/JPA) to the various feature modules and their dependencies. It manages the application's lifecycle,
including graceful startup and shutdown, and applies essential global configurations before the server starts handling
requests.

## Structure

The `server` module is organized into the following packages:

- `config/`: Contains modular configuration classes, subdivided into `startup` and `runtime` configurations, and
  environment-specific settings.
- `context/`: Provides utility classes for managing request-scoped resources and context information, ensuring that
  components throughout the application can access necessary request-specific data without being tightly coupled to the
  web layer.

## Main Classes

- **Application:** The main entry point for the entire application, acting as the ultimate Composition Root. It
  orchestrates the creation of the Javalin server, `PersistenceManager`, and the `DependencyContainer`. It also applies
  environment-specific configurations and registers all the defined routes and middleware.
- **DependencyContainer:** Although not a framework-based solution, this class embodies a **Manual Dependency Injection
  ** approach. It is responsible for instantiating and wiring together all application components (services,
  controllers, repositories) with their respective dependencies. This explicit dependency management makes the
  application's object graph clear and highly testable.
- **Main:** The standard Java `main` class, which initializes and starts the `Application` instance. It handles the
  loading of the application configuration from environment variables or `.env` files and registers a shutdown hook for
  graceful termination.

## Interactions

- **Feature Modules:** The `server` module discovers and registers routes from all feature modules (`auth`, `users`,
  `system`) via their `RouteRegistry` implementations. It also injects dependencies into the controllers and services of
  these features.
- **`persistence` module:** It initializes the `PersistenceManager` and integrates it into the application's lifecycle,
  enabling database access for all other modules.
- **Configuration Management:** It leverages the `config` sub-module to load environment variables, apply server-wide
  settings (e.g., CORS, JSON mapping), and set up plugins (e.g., OpenAPI/Swagger).
- **Request Lifecycle:** Through its `config.modules.runtime` classes (like `LifecycleConfig`), it manages per-request
  resources such as JPA `EntityManager` instances and associates them with the current HTTP request context.

## Design Patterns

- **Composition Root:** The `Application` class acts as the central composition root, where all application services and
  components are wired together.
- **Dependency Injection (Manual):** The `DependencyContainer` implements a manual approach to DI, preferring
  constructor injection for explicit dependency management.
- **Module Pattern:** The `config` sub-module demonstrates a modular approach to server configuration, allowing
  individual concerns to be handled by separate classes.
- **Factory Method:** The `Application.create()` and `Application.buildApplication()` methods can be seen as factory
  methods responsible for constructing fully configured application instances based on different environments.