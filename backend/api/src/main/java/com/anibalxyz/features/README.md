# Features Module

This module contains the core features of the Reconciler application. Each feature is organized into its own package,
following a consistent structure based on Clean Architecture principles.

<details>
<summary>Table of Contents</summary>

| Section                             | Description                                                      |
|:------------------------------------|:-----------------------------------------------------------------|
| [Overview](#overview)               | High-level description of the module's purpose.                  |
| [Structure](#structure)             | Details on how features are organized and layered.               |
| [Features](#features)               | Overview of individual features and their responsibilities.      |
| [Design Patterns](#design-patterns) | Key architectural and design patterns applied within the module. |

</details>

## Overview

The `features` module is the heart of the backend application, encapsulating distinct business capabilities. Each
sub-module within `features` represents a vertical slice of the application, focusing on a specific domain aspect (e.g.,
authentication, user management). This modular approach enhances maintainability, scalability, and testability by
promoting high cohesion within features and loose coupling between them.

## Structure

Each feature package adheres to a layered architecture, typically comprising:

- `api/`: **Presentation Layer**. Defines the RESTful API endpoints (controllers) and their associated Data Transfer
  Objects (DTOs) for request and response payloads. It uses Javalin for routing and OpenAPI annotations for API
  documentation.
- `application/`: **Application Layer**. Contains application services that orchestrate the business logic. These
  services coordinate domain objects and repositories to fulfill use cases. They handle input validation, transaction
  management, and interaction with infrastructure services, but do not contain direct business rules.
- `domain/`: **Domain Layer**. The core of the business logic. It includes aggregate roots (e.g., `User`), value
  objects (e.g., `Email`, `PasswordHash`), and repository interfaces (ports). This layer is independent of any
  infrastructure concerns.
- `infra/`: **Infrastructure Layer**. Provides the concrete implementations (adapters) for the repository interfaces
  defined in the domain layer. This is where persistence technologies (e.g., JPA/Hibernate) are integrated.

## Features

- [**auth**](./auth/README.md): Handles user authentication, including login, logout, JWT generation and validation, and
  refresh token
  management with token rotation. It secures API endpoints based on user roles.
- [**common**](./common/README.md): Provides common classes and utilities shared across features, such as routing
  helpers, standardized exception handling, and role definitions for access control.
- [**system**](./system/README.md): Implements system-level endpoints, primarily for health checks and monitoring the
  application's operational status.
- [**users**](./users/README.md): Manages user-related operations, including full CRUD (Create, Read, Update, Delete)
  functionality, user creation with password hashing, and email uniqueness enforcement.

## Design Patterns

The `features` module extensively utilizes several design patterns to achieve its architectural goals:

- **Clean Architecture / Hexagonal Architecture:** The primary guiding principle, ensuring a clear separation of
  concerns between domain, application, and infrastructure layers. The domain layer remains independent of external
  frameworks and technologies.
- **Repository Pattern:** Abstracted persistence logic through interfaces in the domain layer, with concrete
  implementations in the infrastructure layer. This allows for easy swapping of persistence technologies.
- **Dependency Injection (Manual):** Dependencies are explicitly passed to constructors, making the object graph
  transparent and facilitating testability. The `DependencyContainer` in the `server` module acts as the composition
  root.
- **Value Objects:** Immutable classes like `Email` and `PasswordHash` encapsulate data and their associated validation
  rules, ensuring data integrity.
- **Aggregate Root:** `User` acts as an aggregate root, controlling access to its internal state and ensuring business
  invariants.
- **DTOs (Data Transfer Objects):** Used in the `api` layer to define the contract for data exchange with clients,
  preventing direct exposure of domain models and sensitive information.
