# Reconciler

Reconciler is a lightweight and modular application designed to help teams reconcile financial transactions between bank
statements and internal systems. Built with industry best practices, it aims to provide an intuitive and customizable
platform with powerful utilities for both individual users and collaborative environments.

<details>
<summary>Table of Contents</summary>

- [Planned Features](#planned-features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [CLI Usage](#cli-usage)
- [Accessing the Application](#accessing-the-application)
- [License](#license)

</details>

## Planned Features

- **Initial Setup**: Configure core parameters such as transaction sources, categories, and available log actions. This
  step is required only once after deployment.
- **User Authentication**: Basic login system using predefined credentials. Users can later update their password, but
  only admins can create or manage accounts.
- **Dashboard**: Central hub for navigating application features.
- **Transaction Upload**: Upload data from both bank and internal systems in supported formats.
- **Automated Reconciliation**: Automatically identifies matches and discrepancies between uploaded transactions.
- **Manual Discrepancy Resolution**: Interface for resolving unmatched or ambiguous records.
- **Admin Panel**: Admin-only section to add users and define new system parameters (e.g. categories, sources, action
  types).
- **Reports and Exports**: Export data in PDF, Excel, or other conventional formats.
- **Analytics and Charts**: Visualize financial activity, reconciliation rates, and trends.
- **Public Site**: General access site with product information and login/registration portal.

## Tech Stack

- **Backend**: Java 21 with Javalin
- **Frontend**: TypeScript with Vite
    - **Dashboard**: React
    - **Public Site**: Plain TypeScript and HTML
- **Database**: PostgreSQL with Flyway for migrations
- **Web Server**: Nginx (for production)
- **CLI**: Python 3 with Typer
- **Containerization**: Docker & Docker Compose

## Project Structure

A brief overview of the most important files and directories in the project.

```
.
├── cli/                 # Source code for the Python CLI tool
│   └── main.py          # Entrypoint for the CLI
├── backend/
│   ├── api/             # Java API (Javalin) source code
│   │   └── pom.xml      # Backend dependencies (Maven)
│   └── db/
│       └── migrations/  # Database migrations (Flyway)
├── frontend/
│   ├── dashboard/       # React application for the dashboard
│   └── public-site/     # TypeScript application for the public site
├── nginx/               # Nginx configuration for the production environment
├── compose.yaml         # Base Docker Compose configuration for all services
├── compose.<env>.yaml   # Docker Compose overrides for the <env> environment
├── pyproject.toml       # Project definition and dependencies for the CLI tool
└── README.md            # This file
```

## Prerequisites

- **Git** (to clone the repository).
- **Docker v20.10+** and Docker Compose v2+ (to run the application).
- **Python 3.8+** and **pip** (to use the CLI tool).

> [!TIP]
> This project is designed to be run with Docker, which is the recommended approach. However, if you wish to run the
> services locally on your host machine, you will need to install and configure the following:
>
> - **Java 21** and **Maven**: To build and run the backend API.
> - **Node.js 22+** and a package manager (`npm`, `pnpm`, or `yarn`): To build and run the frontend applications.
> - **PostgreSQL Server**: A running instance for the application to connect to.
> - **Flyway Command-Line Tool**: To run database migrations against your local PostgreSQL instance.
> - **Nginx**: To replicate the production environment's reverse proxy setup.

## Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/anibalxyz/reconciler.git
cd reconciler
```

### 2. Install the CLI

This project is managed by a powerful, custom-built command-line interface. The CLI provides a unified and intuitive way
to manage the application's environments and lifecycle, but we will see it in action below ✨.

The CLI is a Python application. It is recommended to install it in a virtual environment.

```bash
# Create and activate a virtual environment (optional but recommended)
python3 -m venv ./cli/.venv
source ./cli/.venv/bin/activate

# Install the CLI in editable mode
pip install -e .[dev]
```

```bash
# If dont want to use editable mode
pip install .
```

Thanks to [Typer](https://typer.tiangolo.com/), the CLI is fully self-documented, so you can get help for any command or
subcommand by simply adding `--help` to it.

```bash
# Try it out!
cli --help
```

### 3. Set Up The Environment

The CLI can manage different environments (e.g., `dev`, `prod`, `test`). Set your desired environment and initialize the
configuration files in one go using the `--init` flag. This is the recommended way to get started.

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

## Accessing the Application

After running `compose up`, you can access the services at the following URLs.

> [!NOTE]
> The ports listed below are the default values defined in the `.env` files. If you change them, you will need to adjust
> the URLs accordingly.

| Environment | Service             | URL                          |
|:------------|:--------------------|:-----------------------------|
| `dev`       | API                 | http://localhost:4001/health |
| `dev`       | Public Site         | http://localhost:5173/       |
| `dev`       | Dashboard           | http://localhost:5174/       |
| `prod`      | Nginx Reverse Proxy | http://localhost/            |
| `prod`      | API via Nginx       | http://localhost/api/health  |

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for more details.
