# CLI - Full Setup Guide

This guide covers the full setup: prerequisites, CLI installation, environment configuration, and how to build, run, and stop the application. For a quick start, see the [Getting Started](../../README.md#getting-started) section in the README.

## Prerequisites

- **Git** (to clone the repository)
- **Docker v20.10+** and Docker Compose v2+ or v5.1+ (to run the application)
- **Python 3.10+** and **pip** (to use the CLI tool)

> [!WARNING]
> This project is designed to be run with Docker, which is the recommended and officially supported approach. Running services locally on your host machine is partially supported for **API and frontend** (mainly for development), but may require additional manual configuration.
>
> **For local development** (API and frontend services):
>
> - **Java 21** and **Maven**: To build and run the backend API
> - **Node.js 24.16+** and a package manager (`npm`, `pnpm`, or `yarn`): To build and run the frontend applications
> - **PostgreSQL Server**: A running instance for the application to connect to
>
> **Not supported for local execution** (Docker-only services):
>
> - **Flyway**: Database migrations must be run via Docker
> - **Nginx**: Production reverse proxy setup is Docker-only
>
> Local execution support for all services is not currently planned.

## Install the CLI

This project is managed by a custom-built CLI. It is recommended to install it in a virtual environment.

```bash
# Create and activate a virtual environment
python3 -m venv ./cli/.venv
source ./cli/.venv/bin/activate
```

```bash
# Install the CLI. `-e` for editable mode (auto reload on changes)
pip install -e ./cli[dev]
```

Thanks to [Typer](https://typer.tiangolo.com/), the CLI is fully self-documented. Run `cli --help` to explore commands, subcommands, and their options.
Every subcommand also accepts `--help` (e.g. `cli compose --help`), so there's always a way to discover what's available without leaving the terminal.

The sections below show a happy-path example of setting up and running the dev environment.

## Example: Dev environment walkthrough

```bash
# Set the environment and initialize config files from templates
# You will be prompted to fill in values in the `.env.dev` using `nano`
cli set env dev --init
```

```bash
# Build all needed Docker images
cli image build all
```

```bash
# Start all services
cli compose up all
```

```bash
# Stop and remove all containers
cli compose down all
```

## Accessing the Application

> [!NOTE]
> The ports listed below are the default values defined in the `.env.dev.example` files. If you change them, the URLs will differ accordingly.

| Service     | URL                      | Description                              |
| :---------- | :----------------------- | :--------------------------------------- |
| API         | <http://localhost:4001/> | Swagger UI for API documentation         |
| Public Site | <http://localhost:5174/> | Login and registration pages             |
| Dashboard   | <http://localhost:5175/> | Authenticated dashboard (requires login) |

## CLI Architecture

> [!NOTE]
> This section describes how the CLI is built internally for those who want to understand or contribute to the CLI itself. Not required for day-to-day usage.

The CLI lives entirely under `cli/`.
It is a single Python application built with [Typer](https://typer.tiangolo.com/) and installed as a system command via `pyproject.toml`.

### Entry point

`cli/src/reconciler_cli/main.py`:

- Finds the project root by walking up directories until it finds `compose.yaml` (max 20 levels). Fails if not found.
- Changes the working directory to the project root so all paths resolve correctly
- Checks that Docker is installed
- Registers four subcommand groups: `compose`, `image`, `resource`, `set`, `get`

### Module structure

| Module         | Commands                        | What it does                                                                                                                       |
| -------------- | ------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| `compose.py`   | `up`, `down`, `rebuild`, `test` | Wraps `docker compose` with the correct compose files and env files for the current environment                                    |
| `image.py`     | `build`, `push`, `pull`         | Builds/pushes/pulls Docker images. Uses `docker build` directly (not compose) for each service                                     |
| `resource.py`  | `list`, `prune`                 | Lists or prunes Docker resources (images, containers, volumes, networks)                                                           |
| `config.py`    | `set env`, `get env`            | Reads/writes `cli.cfg` (persisted environment selection). `set env --init` also populates `.env.*` files from `.example` templates |
| `constants.py` | _(data)_                        | Service-to-path mappings, available environments, env file paths                                                                   |

### Command assembly flow

The command assembly varies by module:

- **`compose.*` commands**: assemble a `docker compose` invocation with `-f compose.yaml -f compose.{env}.yaml` (plus monitoring files for dev/prod) and `--env-file` flags for backend and frontend dotenv files. All env-specific logic is centralized; individual commands just receive a clean list of services.
- **`image.*` commands**: bypass compose entirely and call `docker build` or `docker push` directly. The build context and Dockerfile are resolved per service based on the environment (e.g. `Dockerfile.dev` for dev, `Dockerfile.prod` for prod, falling back to `Dockerfile`). Services are defined in `constants.py` as paths relative to the project root.
- **`resource.*` commands**: plain `docker` calls (`docker images ls`, `docker container prune`, etc.) with no compose involvement.
- **`config.*` commands**: local file operations on `cli.cfg` and `.env.*` files. No Docker interaction.

### Key files the CLI depends on

| File / pattern                    | Purpose                                                          |
| --------------------------------- | ---------------------------------------------------------------- |
| `cli.cfg`                         | Persists the current environment (`ENV = dev` / `prod` / `test`) |
| `compose.yaml`                    | Base compose file (all environments share this)                  |
| `compose.{env}.yaml`              | Environment-specific compose overrides                           |
| `backend/.env.{env}`              | Backend environment variables                                    |
| `frontend/.env.{env}`             | Frontend environment variables                                   |
| `backend/.env.{env}.example`      | Templates for creating missing `.env.*` files (`--init`)         |
| `frontend/.env.{env}.example`     | Same as above, for frontend                                      |
| `Dockerfile` / `Dockerfile.{env}` | Dockerfiles per service and environment                          |
