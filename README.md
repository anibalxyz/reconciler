# Reconciler

Reconciler is a lightweight and modular application designed to help teams reconcile financial transactions between bank statements and internal systems. Built with industry best practices, it aims to provide an intuitive and customizable platform with powerful utilities for both individual users and collaborative environments.

<details>
<summary>Table of Contents</summary>

- [Technology Stack](#technology-stack)
- [Planned Features](#planned-features)
- [How to Run](#how-to-run)
  - [Prerequisites](#1-prerequisites)
  - [Relevant Files](#2-relevant-files)
  - [Commands and Workflow](#3-commands-and-workflow)
  - [Running the Application](#4-running-the-application)
  - [Stopping the Application](#5-stopping-the-application)
  - [Other Useful Commands](#6-other-useful-commands)
- [License](#license)

</details>

## Technology Stack

- **Backend**: Java 21 (using embedded Jetty)
- **Frontend**: React + Vite + TypeScript
- **Database**: PostgreSQL
- **Containerization**: Docker & Docker Compose
- **Automation & Tooling**: GNU Make, custom `Makefile` for environment and workflow management

## Planned Features

- **Initial Setup**: Configure core parameters such as transaction sources, categories, and available log actions. This step is required only once after deployment.
- **User Authentication**: Basic login system using predefined credentials. Users can later update their password, but only admins can create or manage accounts.
- **Dashboard**: Central hub for navigating application features.
- **Transaction Upload**: Upload data from both bank and internal systems in supported formats.
- **Automated Reconciliation**: Automatically identifies matches and discrepancies between uploaded transactions.
- **Manual Discrepancy Resolution**: Interface for resolving unmatched or ambiguous records.
- **Admin Panel**: Admin-only section to add users and define new system parameters (e.g. categories, sources, action types).
- **Reports and Exports**: Export data in PDF, Excel, or other conventional formats.
- **Analytics and Charts**: Visualize financial activity, reconciliation rates, and trends.

## How to Run

### 1. Prerequisites

- **Git** (to clone the repository).
- **Docker v20.10+** and **Docker Compose v2+** (to run the application in a containerized environment).
- **Make** (available by default in most modern Linux distributions).
- **Java 21** (only if you plan to build or run locally outside of Docker).

### 2. Relevant Files

- `Makefile`: Contains commands to simplify developer workflow.
- `compose.yaml`: Defines the common base for all environments.
- `compose.<environment>.yaml`: Defines the specific environment configuration.
- `Dockerfile` & `Dockerfile.dev`: Define how to build the application image (currently only available for backend).
- `.env.<environment>.example`: Template for environment variables used in the application.

### 3. Commands and Workflow

All you need to work with this project is the `Makefile`. It provides a set of commands to manage the application lifecycle.
All commands are documented in the `Makefile` itself. You can view them by running:

```bash
make help
# Or just:
make
```

### 4. Running the Application

Follow these steps to run the application:

0. Clone the repository and navigate to the project directory:

```bash
git clone https://github.com/anibalxyz/reconciler.git
cd reconciler

# Or if you prefer SSH:
git clone git@github.com:anibalxyz/reconciler.git
cd reconciler
```

1. Set the desired environment:

```bash
make set-env NEW_ENV=<environment>

# For example, to set the environment to 'development':
make set-env NEW_ENV=development
```

2. Bring up the application:

```bash
make up
```

> [!NOTE]
> If you have not created the `.env.<environment>` file yet, the command will automatically create it based on the `.env.<environment>.example` template. You will then be prompted to edit it using `nano` before starting the environment. In future releases, you’ll be able to configure your own editor instead of nano. If you reject the prompt, the application will not start, and you will need to modify the `.env.<environment>` file manually.

3. Test the API:

```bash
curl http://localhost:<API_HOST_PORT>/api/health

# For example, if you set API_HOST_PORT=8081 in your .env file, you would run:
curl http://localhost:8081/api/health
```

You will receive a response like this if everything is working correctly:

```json
{
  "status": true
}
```

### 5. Stopping the Application

To stop the selected environment, you can run:

```bash
make down
```

### 6. Other Useful Commands

Apart from the commands used above, the `Makefile` provides several other commands with various functionalities. Some of the most useful ones include:

- `make logs`: View the logs of the application.
- `make rebuild`: Rebuild the API image.
- `make restart`: Restart the running containers (useful when changes were made in code during development).
- `make list-<scope>`: List Docker resources such as containers and images. You can also list all resources with `make list-all`.
- `make prune-<scope>`: Remove unused Docker resources such as containers and images. You can also remove all unused resources with `make prune-all`. Use with **caution**, as this will remove all stopped containers and unreferenced images.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for more details.
