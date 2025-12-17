# Features Test Module

This module contains tests for the core features of the application, providing a robust and consistent testing
environment.

<details>
<summary>Table of Contents</summary>

| Section                             | Description                                                   |
|:------------------------------------|:--------------------------------------------------------------|
| [Overview](#overview)               | High-level description of the module's purpose.               |
| [Structure](#structure)             | Details on how tests are organized within the module.         |
| [Main Components](#main-components) | Description of key utility classes and their functionalities. |

</details>

## Overview

This module is dedicated to ensuring the quality and correctness of the core business features of the Reconciler
application. It provides a structured approach to testing, mirroring the main application's feature-based organization.
This setup facilitates easy navigation, promotes test isolation, and supports efficient development and maintenance of
test suites.

## Structure

Tests within this module are organized by feature, directly reflecting the package structure of the main source code.
This ensures a clear separation of concerns and easy navigation, allowing developers to quickly locate tests relevant to
a specific feature.

- **`auth/`**: This sub-package contains comprehensive tests for the authentication feature. It includes:
    - **Unit Tests:** For individual components like `AuthService`, `JwtService`, and `RefreshTokenService`, ensuring
      their internal logic functions correctly.
    - **Integration Tests:** For the `AuthRoutes` and `JwtMiddleware`, verifying the correct behavior of API endpoints
      related to login, token refresh, and JWT middleware functionality. These tests interact with the actual HTTP layer
      and database, simulating real client requests.

- **`users/`**: This sub-package contains extensive tests for the user management feature. It covers:
    - **Unit Tests:** For `UserService` and domain objects like `User`, `Email`, and `PasswordHash`, validating business
      rules and data integrity.
    - **Integration Tests:** For the `UserRoutes`, confirming the correct operation of user CRUD (Create, Read, Update,
      Delete) API endpoints. These tests interact with the actual HTTP layer and database.

## Main Classes

This module centralizes common testing utilities to promote reusability and maintain consistency across all test suites.

- **Constants:** (`Constants.java`)
    - **Purpose:** Provides centralized constants for testing, including environment configuration and mock data.
    - **Key Functionalities:**
        - `APP_CONFIG`: Stores application configuration loaded specifically for the test environment.
        - `Environment` nested class: Holds environment-specific settings like `BCRYPT_LOG_ROUNDS`, `JWT_SECRET`,
          `JWT_ISSUER`, and JWT expiration times.
        - `Users` nested class: Defines valid user data for consistent test scenarios.
        - `Auth` nested class: Provides valid JWT and refresh token strings for authentication tests.
    - **Usage:** Ensures that tests use consistent and valid data, improving maintainability and reducing redundancy.

- **Helpers:** (`Helpers.java`)
    - **Purpose:** A utility class providing helper methods for mocking, argument capturing, and database state
      management during tests.
    - **Key Functionalities:**
        - `stubBodyValidatorFor(Context ctx, Class<T> clazz)`: Stubs the behavior of Javalin's `BodyValidator`.
        - `capturedJsonAs(Context ctx, Class<T> clazz)`: Captures JSON arguments passed to `Context.json()`.
        - `capturedCookie(Context ctx)`: Captures `Cookie` arguments passed to `Context.cookie()`.
        - `cleanDatabase(EntityManager em)`: Truncates all tables in the public schema to ensure a clean test state.
        - `capitalize(String s)`: Capitalizes the first letter of a string.
        - `persistUser(EntityManager em, String name, String email)`: Persists a new user to the database for testing.
    - **Usage:** Simplifies common test setup and assertion patterns, reducing boilerplate code.

- **HttpRequest:** (`HttpRequest.java`)
    - **Purpose:** A utility class for making HTTP requests in integration tests, providing a convenient wrapper around
      OkHttp.
    - **Key Functionalities:**
        - `get(String path)`: Sends an HTTP GET request.
        - `post(String path, Object body)`: Sends an HTTP POST request with a JSON body.
        - `put(String path, Object body)`: Sends an HTTP PUT request with a JSON body.
        - `delete(String path)`: Sends an HTTP DELETE request.
        - `parseBody(Response response, TypeReference<T> typeRef)`: Parses HTTP response bodies into specified Java
          types.
    - **Usage:** Facilitates easy and consistent interaction with the API endpoints during integration testing.