# com.anibalxyz Package

This is the root package for the `com.anibalxyz` domain within the backend API. It serves as the top-level
organizational unit for all application components, adhering to a modular and layered architectural style.

<details>
<summary>Table of Contents</summary>

| Section                 | Description                                                  |
|:------------------------|:-------------------------------------------------------------|
| [Overview](#overview)   | High-level description of the `com.anibalxyz` package.       |
| [Structure](#structure) | Details on the main sub-packages and their responsibilities. |

</details>

## Overview

The `com.anibalxyz` package is designed to encapsulate all the core logic and infrastructure of the Reconciler backend
application. It follows a clear separation of concerns, dividing the application into distinct modules based on their
functional responsibilities and architectural layers. This structure promotes maintainability, scalability, and
testability, making it easier to understand and evolve the codebase.

## Structure

The `com.anibalxyz` package is primarily divided into the following high-level sub-packages:

- [**features/**](./features/README.md): Contains the core business features of the application, such as authentication,
  user management, and system health checks. Each feature is further organized into its own layered structure (api,
  application, domain, infra).
- [**persistence/**](./persistence/README.md): Manages the application's data persistence layer, including database
  connections, JPA `EntityManagerFactory`, and `EntityManager` lifecycle. It abstracts the underlying database
  technology from the application logic.
- [**server/**](./server/README.md): Acts as the application's composition root, responsible for initializing and wiring
  together all major components, configuring the Javalin web server, and managing the application's lifecycle.
