# Server Context Module

This module provides utilities for managing the request context, allowing components in different layers to access
request-specific data without direct dependencies on the web layer.

<details>
<summary>Table of Contents</summary>

| Section                       | Description                                                  |
|:------------------------------|:-------------------------------------------------------------|
| [Overview](#overview)         | High-level description of the server context module.         |
| [Main Classes](#main-classes) | Key classes involved in managing the server request context. |

</details>

## Overview

The `context` module provides essential utilities for managing the request-scoped context within the Javalin
application. It allows components deep within the application layers (e.g., persistence repositories) to access
request-specific data, such as the `EntityManager`, without needing the `Context` object to be explicitly passed down
through every method call. This mechanism is crucial for maintaining a clean architecture and decoupling layers from the
web framework.

## Main Classes

- **ContextProvider:** A utility class that provides request-scoped access to the Javalin `Context` using a
  `ThreadLocal`.
- **JavalinContextEntityManagerProvider:** An implementation of `EntityManagerProvider` that retrieves the
  `EntityManager` from the Javalin request context, decoupling the persistence layer from the web framework.
