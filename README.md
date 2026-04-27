# Reconciler

[🇪🇸 Versión en español](/README.es.md)

Reconciler is a lightweight and modular application designed to help teams reconcile financial transactions between bank
statements and internal systems. Built with industry best practices, it aims to provide an intuitive and customizable
platform with powerful utilities for both individual users and collaborative environments.

<details>
<summary>Table of Contents</summary>

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [License](#license)

</details>

## Features

**Legend**:

- ✅ **Done** - Feature implemented and functional
- 🔨 **Done+** - Feature implemented with known improvement opportunities
- 🚧 **Doing** - Currently in development
- 📋 **Todo** - Planned for future implementation

---

- 🚧 **Public Site**: General access site with login/registration portal using Astro for SEO-optimized SSR
  - *Current state*: Technical infrastructure complete, temporary placeholder content and navbar
- 🔨 **User Authentication**: JWT-based authentication with refresh token rotation, time-window access control (Mon-Fri 08:00-20:00), and logout functionality
  - *Known improvements*: Additional time-window rules, revoked cookie blocking mechanisms
- 🚧 **Dashboard**: React-based authenticated central hub for navigating application features
  - *Current state*: Authentication flow complete, temporary Swagger UI link placeholder
- 🔨 **User Management**: CRUD operations for user accounts with role-based access control
  - *Known improvements*: Pagination, restrict creation to admins only
- ✅ **API Documentation**: Interactive Swagger UI with OpenAPI specifications for all endpoints
- 📋 **Initial Setup**: Configure core parameters such as transaction sources, categories, and available log actions (required once after deployment)
- 📋 **Transaction Upload**: Upload data from both bank and internal systems in supported formats
- 📋 **Automated Reconciliation**: Automatically identifies matches and discrepancies between uploaded transactions
- 📋 **Manual Discrepancy Resolution**: Interface for resolving unmatched or ambiguous records
- 📋 **Admin Panel**: Admin-only section to define new system parameters (e.g. categories, sources, action types)
- 📋 **Reports and Exports**: Export data in PDF, Excel, or other conventional formats
- 📋 **Analytics and Charts**: Visualize financial activity, reconciliation rates, and trends

## Tech Stack

- **Backend**: Java 21 with Javalin
- **Frontend**: TypeScript + TailwindCSS
  - **Dashboard**: Vite + React
  - **Public Site**: Astro
- **Database**: PostgreSQL with Flyway for migrations
- **Web Server**: Nginx (for production)
- **CLI**: Python 3 with Typer
- **Containerization**: Docker & Docker Compose

## Project Structure

A brief overview of the most important files and directories in the project:

```text
.
├── cli/                 # Python CLI tool
│   ├── src/             # Source code
│   │   └── modules/     # CLI command modules
│   └── pyproject.toml   # Project definition and dependencies
├── backend/
│   ├── api/             # Java API (Javalin) source code
│   │   └── pom.xml      # Backend dependencies (Maven)
│   └── db/
│       └── migrations/  # Database migrations (Flyway)
├── frontend/
│   ├── common/          # Shared utilities and services
│   ├── dashboard/       # React application for the dashboard
│   └── public-site/     # Astro application for the public site
├── nginx/               # Nginx configuration for the production environment
├── compose.yaml         # Base Docker Compose configuration for all services
├── compose.<env>.yaml   # Docker Compose overrides for the <env> environment
└── README.md            # This file
```

## Getting Started

### Prerequisites

- **Git** (to clone the repository)
- **Docker v20.10+** and Docker Compose v2+ (to run the application). **Do NOT use v5.x**
- **Python 3.10+** and **pip** (to use the CLI tool)

> [!WARNING]
> This project is designed to be run with Docker, which is the recommended and officially supported approach. Running services locally on your host machine is partially supported for **API and frontend** (mainly for development), but may require additional manual configuration.
>
> **For local development** (API and frontend services):
>
> - **Java 21** and **Maven**: To build and run the backend API
> - **Node.js 22+** and a package manager (`npm`, `pnpm`, or `yarn`): To build and run the frontend applications
> - **PostgreSQL Server**: A running instance for the application to connect to
>
> **Not supported for local execution** (Docker-only services):
>
> - **Flyway**: Database migrations must be run via Docker
> - **Nginx**: Production reverse proxy setup is Docker-only
>
> Local execution support for all services is not currently planned.

### 1. Clone the Repository

```bash
git clone https://github.com/anibalxyz/reconciler.git

# All the following commands will assume you are in the project root
cd reconciler
```

### 2. Install the CLI

This project is managed by a powerful, custom-built command-line interface. The CLI provides a unified and intuitive way to manage the application's environments and lifecycle.

The CLI is a Python application. It is recommended to install it in a virtual environment.

```bash
# Create and activate a virtual environment
python3 -m venv ./cli/.venv
source ./cli/.venv/bin/activate

# Install the CLI in editable mode
pip install -e ./cli[dev]
```

```bash
# If you don't want to use editable mode
pip install ./cli
```

Thanks to [Typer](https://typer.tiangolo.com/), the CLI is fully self-documented, so you can get help for any command or subcommand by simply adding `--help` to it.

```bash
# Try it out!
cli --help
```

### 3. Set Up The Environment

The CLI can manage different environments (e.g., `dev`, `prod`, `test`). Set your desired environment and initialize the configuration files in one go using the `--init` flag. This is the recommended way to get started.

```bash
# Syntax: cli set env <environment> --init
cli set env dev --init
```

This command will:

1. Persist the chosen environment in the `cli.cfg` file.
2. If they don't exist, create `.env.*` files from their `.example` templates and prompt you to edit them with `nano`.

### 4. Run the Application

First, build the Docker images for all services in the current environment.

```bash
cli image build all
```

Then, start the services using Docker Compose.

```bash
cli compose up all
```

### 5. Stop the Application

To stop and remove all running containers and networks, use `compose down`.

```bash
cli compose down all
```

### Accessing the Application

After running `compose up`, you can access the services at the following URLs.

> [!NOTE]
> The ports listed below are the default values defined in the `.env` files. If you change them, you will need to adjust the URLs accordingly.

| Environment | Service            | URL                      | Description                                      |
| :-----------| :------------------| :------------------------| :------------------------------------------------|
| `dev`       | API                | <http://localhost:4001/> | Swagger UI for API documentation                 |
| `dev`       | Public Site        | <http://localhost:5174/> | Login and registration pages                     |
| `dev`       | Dashboard          | <http://localhost:5175/> | Authenticated dashboard (requires login)         |
| `prod`      | Frontend via Nginx | <http://localhost/>      | Public site and dashboard                        |
| `prod`      | API via Nginx      | <http://localhost/api/>  | API and Swagger UI                               |

> [!TIP]
> To access the Dashboard, you'll need to log in through the Public Site first. A temporary Swagger UI link is available in the Dashboard for API exploration.

## License

This project is licensed under the MIT License. See the [LICENSE](/LICENSE) file for more details.

## 💰 Bounty Contribution

- **Task:** Standardize API Error Responses
- **Reward:** $50
- **Source:** GitHub-Paid
- **Date:** 2026-04-28

