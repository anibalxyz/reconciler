# System Feature

This feature provides system-level endpoints, primarily for monitoring the application's health and operational status.

<details>
<summary>Table of Contents</summary>

| Section                       | Description                                          |
|:------------------------------|:-----------------------------------------------------|
| [Overview](#overview)         | High-level description of the system feature.        |
| [Structure](#structure)       | Details on how the system feature is organized.      |
| [Main Classes](#main-classes) | Key classes involved in the system feature.          |
| [Interactions](#interactions) | How the system feature interacts with other modules. |

</details>

## Overview

The `system` feature is a crucial component for ensuring the reliability and availability of the application. It exposes
endpoints that allow external monitoring systems or administrators to quickly assess the application's operational
status, including its connectivity to essential services like the database. This helps in proactive identification and
resolution of potential issues.

## Structure

The `system` feature is organized into the following package:

- `api/`: **Presentation Layer**. Contains the `SystemController` which implements the `SystemApi` interface, defining
  the REST endpoint for the health check. It also includes the `HealthResponse` DTO for the health check's output.

## Main Classes

- **SystemController:** Implements the `SystemApi` interface. Its primary responsibility is to handle the `/health`
  endpoint, which performs a check on the application's database connection status.
- **SystemApi:** Defines the API contract for system-level operations, using OpenAPI annotations to document the health
  check endpoint.
- **SystemRoutes:** Registers the system-related routes with the Javalin server, mapping the `/health` endpoint to the
  `SystemController`.
- **HealthResponse:** A Data Transfer Object (DTO) that represents the response body for the health check endpoint,
  indicating whether the database is connected.

## Interactions

- **`persistence` module:** The `SystemController` directly interacts with the `PersistenceManager` to verify the
  database connection. This interaction is a key part of determining the overall health of the application.
- **Javalin Server:** The `SystemRoutes` class integrates with the main Javalin application to make the `/health`
  endpoint accessible. This endpoint is typically public and does not require authentication.
