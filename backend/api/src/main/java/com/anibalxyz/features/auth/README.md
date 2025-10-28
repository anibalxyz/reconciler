# Auth Feature

This feature is responsible for handling user authentication, including login, token generation, and token refreshing.
It implements a robust and secure authentication flow using JSON Web Tokens (JWTs) and refresh tokens.

<details>
<summary>Table of Contents</summary>

| Section                                     | Description                                                  |
|:--------------------------------------------|:-------------------------------------------------------------|
| [Overview](#overview)                       | High-level description of the authentication feature.        |
| [Structure](#structure)                     | Details on how the authentication feature is organized.      |
| [Main Classes](#main-classes)               | Key classes involved in the authentication process.          |
| [Interactions](#interactions)               | How the authentication feature interacts with other modules. |
| [Authentication Flow](#authentication-flow) | Step-by-step explanation of the authentication process.      |

</details>

## Overview

The `auth` feature provides the necessary mechanisms for users to securely access the application. It focuses on issuing
and validating access tokens, managing long-lived refresh tokens for seamless re-authentication, and enforcing access
control to protected resources. The implementation prioritizes security best practices, such as token rotation and
HttpOnly cookies for refresh tokens.

## Structure

The `auth` feature is organized into the following packages, adhering to the Clean Architecture principles:

- `api/`: **Presentation Layer**. Contains the `AuthController` which exposes REST endpoints for user login and token
  refreshing. It also defines the `AuthApi` interface, DTOs like `LoginRequest`, and OpenAPI examples for API
  documentation.
- `application/`: **Application Layer**. Houses the core authentication logic within `AuthService`. It orchestrates
  interactions between user services, JWT services, and refresh token services. This layer also defines environment
  interfaces (`JwtEnvironment`, `RefreshTokenEnvironment`) and application-specific exceptions (
  `InvalidCredentialsException`).
- `domain/`: **Domain Layer**. Defines the `RefreshToken` entity, which represents a long-lived token for
  re-authentication, and the `RefreshTokenRepository` interface, abstracting persistence concerns.
- `infra/`: **Infrastructure Layer**. Provides the JPA implementation (`JpaRefreshTokenRepository`) for persisting and
  managing `RefreshToken` entities in the database.

## Main Classes

- **AuthController:** Implements the `AuthApi` interface, handling HTTP requests for user login and token refreshing. It
  validates incoming requests and delegates business logic to the `AuthService`.
- **AuthService:** The central application service for authentication. It verifies user credentials, generates JWT
  access tokens, and manages refresh token creation and rotation. It relies on `UserService`, `JwtService`, and
  `RefreshTokenService`.
- **JwtService:** Responsible for generating, validating, and extracting claims from JWT access tokens. It uses a
  configurable secret key and expiration time.
- **RefreshTokenService:** Manages the lifecycle of refresh tokens. This includes creating new tokens, verifying
  existing ones, implementing token rotation (revoking old tokens and issuing new ones), and cleaning up expired tokens.
- **JwtMiddleware:** A Javalin `Handler` that acts as a global authentication filter. It intercepts requests, validates
  JWTs from the `Authorization` header, extracts user IDs, and enforces role-based access control.

## Interactions

- **`users` feature:** The `AuthService` depends on the `UserService` (from the `users` feature) to retrieve user
  details and verify passwords during the login process.
- **`persistence` module:** The `JpaRefreshTokenRepository` interacts directly with the database via the `persistence`
  module to store, retrieve, and manage refresh tokens.
- **Global Middleware:** The `JwtMiddleware` is integrated into the Javalin server's request lifecycle to protect
  authenticated routes, ensuring that only authorized users can access specific resources.
- **Scheduler:** The `SchedulerConfig` in the `server.config.modules.runtime` package schedules periodic cleanup of
  expired refresh tokens using the `RefreshTokenService`.

## Authentication Flow

1. **Login Request:** A client sends a `POST` request to `/auth/login` with user credentials (`email`, `password`).
2. **Credential Verification:** The `AuthController` receives the request, validates the input, and passes it to
   `AuthService`.
3. **User Lookup & Password Check:** `AuthService` uses `UserService` to find the user by email and verifies the
   provided password against the stored hash.
4. **Token Generation:** If credentials are valid, `AuthService` generates a short-lived JWT access token via
   `JwtService` and a long-lived refresh token via `RefreshTokenService`.
5. **Response:** The `AuthController` returns the access token in the response body and sets the refresh token as a
   secure, HttpOnly cookie.
6. **Protected Resource Access:** For subsequent requests to protected endpoints, the client includes the JWT access
   token in the `Authorization` header.
7. **JWT Validation:** The `JwtMiddleware` intercepts these requests, validates the JWT using `JwtService`, and extracts
   the user ID.
8. **Token Refresh:** When the access token expires, the client can send a `POST` request to `/auth/refresh` (with the
   refresh token cookie). `AuthService` verifies the refresh token, revokes it, issues a new access token and a new
   refresh token (token rotation), and sends them back to the client.
