# Reconciler

Reconciler is a simple Java-based application that helps reconcile bank statements with company transactions.

## Purpose

This project aims to automate the reconciliation of transactions between a bank's statement and an internal system's records.

### Features:

- **Transaction Comparison**: Compares transactions from the bank statement and the internal system.
- **Conciliation Status**: Indicates whether transactions match or if there are discrepancies.

## How to Run

### Prerequisites

Before running the application, ensure you have Docker and Docker Compose installed on your machine.

### Makefile automatization

This project uses a `Makefile` to simplify all Docker-related tasks for development and production. It includes, for example:

- Commands to start and rebuild the environment.
- Logic to automatically create the required .env files from their corresponding .example templates if they do not exist:

  - .env → used for development
  - .env.production → used for production

For a full list of available commands and their descriptions, check the Makefile itself; everything is documented there.

### Development Environment

To start the development environment, use the following commands:

```bash
make dev
```

To rebuild and start development containers (if you've changed the Dockerfile or source code):

```bash
make dev-rebuild
```

_This uses `compose.override.yaml` and `Dockerfile.dev` for a hot‑reload dev setup._

### Production Environment

To start the production environment:

```bash
make prod
```

To rebuild and start production containers (if you've changed the Dockerfile or source code):

```bash
make prod-rebuild
```

_This uses `compose.yaml` and the production `Dockerfile`._
