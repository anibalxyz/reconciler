# Users Feature

This feature is responsible for managing user-related operations, including creating, reading, updating, and deleting
users. It forms a foundational part of the application's security and access control.

<details>
<summary>Table of Contents</summary>

| Section                             | Description                                                       |
|:------------------------------------|:------------------------------------------------------------------|
| [Overview](#overview)               | High-level description of the user management feature.            |
| [Structure](#structure)             | Details on how the user management feature is organized.          |
| [Main Classes](#main-classes)       | Key classes involved in user management.                          |
| [Interactions](#interactions)       | How the user management feature interacts with other modules.     |
| [Design Patterns](#design-patterns) | Key architectural and design patterns applied within the feature. |

</details>

## Overview

The `users` feature provides a complete set of functionalities for managing user accounts within the Reconciler
application. It adheres strictly to Clean Architecture principles, ensuring a clear separation of concerns between the
presentation, application, domain, and infrastructure layers. This design promotes maintainability, testability, and
flexibility, allowing for independent evolution of each layer.

## Structure

The `users` feature is organized into the following packages:

- `api/`: **Presentation Layer**. Contains the `UserController` which exposes RESTful endpoints for user CRUD
  operations. It also defines the `UserApi` interface, various DTOs (`UserCreateRequest`, `UserUpdateRequest`,
  `UserDetailResponse`), and OpenAPI examples for API documentation. The `UserMapper` handles conversions between domain
  models and DTOs.
- `application/`: **Application Layer**. Includes the `UserService`, which orchestrates the business logic for user
  operations. It handles input validation, interacts with the `UserRepository`, and applies business rules. It also
  defines the `UsersEnvironment` interface for environment-specific configurations (e.g., BCrypt rounds) and input
  payloads (`UserUpdatePayload`).
- `domain/`: **Domain Layer**. The core of the user management logic. It defines the `User` aggregate root, which is an
  immutable entity encapsulating user data and behavior. It also includes value objects like `Email` (with validation)
  and `PasswordHash` (for secure password storage), and the `UserRepository` interface (a port for persistence).
- `infra/`: **Infrastructure Layer**. Provides the JPA implementation (`JpaUserRepository`) of the `UserRepository`
  interface. This layer handles the actual database interactions, mapping domain `User` objects to `UserEntity` JPA
  entities.

## Main Classes

- **UserController:** Implements the `UserApi` interface, handling all HTTP requests related to users. It parses
  requests, delegates to the `UserService`, and formats HTTP responses.
- **UserService:** The application service for user-related use cases. It contains the application-specific business
  logic for creating, retrieving, updating, and deleting users, ensuring data integrity and applying validation rules.
- **User:** The central domain entity representing a user. It is designed as an immutable class, with `with...` methods
  for creating new instances with updated values.
- **Email:** A value object encapsulating a user's email address, including validation and normalization logic.
- **PasswordHash:** A value object representing a hashed password, handling BCrypt hashing and verification.
- **UserRepository:** An interface (port) defining the contract for user data persistence, abstracting the underlying
  storage mechanism.
- **JpaUserRepository:** The JPA-based adapter that implements the `UserRepository` interface, bridging the domain layer
  with Hibernate/JPA for database operations.

## Interactions

- **`auth` feature:** The `users` feature is a fundamental dependency for the `auth` feature, as authentication
  processes require retrieving and validating user information.
- **`persistence` module:** The `JpaUserRepository` interacts directly with the `persistence` module to manage user data
  in the database.
- **API Layer:** The `UserController` exposes the user functionalities through a RESTful API, interacting with the
  `application` layer to perform operations.

## Design Patterns

- **Clean Architecture / Hexagonal Architecture:** The entire feature is structured around these principles, ensuring a
  clear separation of concerns and testability.
- **Repository Pattern:** Used to abstract the data persistence logic, allowing the domain and application layers to
  remain independent of the database technology.
- **Value Object:** `Email` and `PasswordHash` are prime examples, encapsulating data and behavior related to their
  specific concepts.
- **Aggregate Root:** `User` acts as an aggregate root, ensuring consistency within its boundaries.
- **DTOs (Data Transfer Objects):** Used extensively in the `api` layer (`UserCreateRequest`, `UserUpdateRequest`,
  `UserDetailResponse`) to define the data contract with clients and prevent direct exposure of domain models.
- **Mapper:** `UserMapper` is used to translate between domain objects and API-specific DTOs, maintaining separation
  between layers.
