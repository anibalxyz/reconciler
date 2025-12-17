# Persistence Module

This module is responsible for managing the persistence layer of the application, including database connections and the
JPA `EntityManager`.

<details>
<summary>Table of Contents</summary>

| Section                       | Description                                                               |
|:------------------------------|:--------------------------------------------------------------------------|
| [Overview](#overview)         | High-level description of the persistence module.                         |
| [Interactions](#interactions) | How the persistence module interacts with other parts of the application. |

</details>

## Overview

The `persistence` module is responsible for abstracting and managing the application's data storage concerns. It
provides the necessary infrastructure to connect to a database, manage JPA `EntityManagerFactory` and `EntityManager`
instances, and handle database-related configurations. This module ensures that the application and domain layers remain
decoupled from the specific persistence technology used (Hibernate/JPA with PostgreSQL).

- **PersistenceManager:** A key class that manages the lifecycle of the JPA `EntityManagerFactory`. It configures and
  creates a singleton `EntityManagerFactory` using Hibernate and a HikariCP connection pool.
- **DatabaseVariables:** A record that provides a type-safe representation of database connection variables loaded from
  the environment.
- **EntityManagerProvider:** An interface that defines a contract for providing a request-scoped `EntityManager`.

## Interactions

- The `PersistenceManager` is initialized in the `Application` class and is used to create `EntityManager` instances for
  each request.
- Repositories in the `infra` layer of each feature (e.g., `JpaUserRepository`) use an `EntityManagerProvider` to get
  the current `EntityManager` and perform database operations.
