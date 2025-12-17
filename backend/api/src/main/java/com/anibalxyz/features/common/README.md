# Common Module

This module provides shared classes and utilities that are used across multiple features, promoting code reuse and
consistency throughout the application.

<details>
<summary>Table of Contents</summary>

| Section                             | Description                                                      |
|:------------------------------------|:-----------------------------------------------------------------|
| [Overview](#overview)               | High-level description of the common module's purpose.           |
| [Structure](#structure)             | Details on how the common module is organized.                   |
| [Main Components](#main-components) | Key classes and utilities provided by the common module.         |
| [Design Patterns](#design-patterns) | Key architectural and design patterns applied within the module. |

</details>

## Overview

The `common` module serves as a foundational layer for cross-cutting concerns that are not specific to any single
business feature. It centralizes common API constructs, exception handling mechanisms, and routing utilities, ensuring a
standardized approach to these aspects across the entire backend application. This reduces duplication and simplifies
the development and maintenance of new features.

## Structure

The `common` module is organized into the following packages:

- `api/`: Contains components directly related to the API layer, such as routing helpers, role definitions for access
  control, and standardized error response structures.
- `application/`: Includes a hierarchy of custom application-level exceptions that provide semantic meaning to errors
  occurring within the business logic.

## Main Components

- **RouteGroup and RouteRegistry:**
    - **Purpose:** These classes provide a modular and fluent API for defining and registering groups of related HTTP
      routes with the Javalin server. `RouteRegistry` is an abstract base class that feature modules extend to register
      their specific endpoints, while `RouteGroup` simplifies the definition of routes under a common path prefix.
    - **Usage:** Enables a clean and organized way to define API endpoints, improving readability and maintainability of
      the routing configuration.

- **ErrorResponse and ErrorResponseExamples:**
    - **Purpose:** `ErrorResponse` defines a standardized JSON structure for returning error messages to clients,
      including a high-level error summary and a list of detailed explanations. `ErrorResponseExamples` provides
      reusable JSON string examples for OpenAPI documentation, ensuring consistency in error reporting.
    - **Usage:** Ensures that all API error responses adhere to a uniform format, making it easier for client
      applications to handle and display errors.

- **ApplicationException Hierarchy:**
    - **Purpose:** `ApplicationException` serves as the base class for all custom, application-specific exceptions (
      e.g., `InvalidInputException`, `ResourceNotFoundException`, `ConflictException`, `InvalidCredentialsException`).
      This hierarchy provides semantic meaning to business-level errors.
    - **Usage:** These exceptions are typically thrown in the application and domain layers and are caught by the global
      exception handler (`ExceptionsConfig`) in the server configuration, which translates them into appropriate HTTP
      status codes and `ErrorResponse` payloads.

- **Role:**
    - **Purpose:** An enum that defines the different user roles (`GUEST`, `AUTHENTICATED`) used by the access
      management system to control access to API endpoints.
    - **Usage:** Integrated with Javalin's security mechanisms (e.g., `JwtMiddlewareConfig`) to implement role-based
      access control, ensuring that only authorized users can access specific routes.

## Design Patterns

- **Module Pattern:** The `common` module itself acts as a module, encapsulating related functionalities that are shared
  across the application.
- **Factory Method (Implicit):** The `ErrorResponse` class provides a convenience constructor that can be seen as a
  simplified factory for creating error responses.
- **Strategy Pattern (Implicit):** The `RouteRegistry` allows different feature modules to implement their own route
  registration strategy.
