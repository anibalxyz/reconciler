# Reconciler

Reconciler is a lightweight and modular application designed to help teams reconcile financial transactions between bank statements and internal systems. Built with industry best practices, it aims to provide an intuitive and customizable platform with powerful utilities for both individual users and collaborative environments.

<details>
<summary>Table of Contents</summary>

- [Reconciler](#reconciler)
  - [Technology Stack](#technology-stack)
  - [Planned Features](#planned-features)
  - [Prerequisites](#prerequisites)
  - [Relevant Files](#relevant-files)
  - [Running the Application](#running-the-application)
    - [Commands and Workflow Management](#commands-and-workflow-management)
    - [How to Run](#how-to-run)
    - [Accessing the Application](#accessing-the-application)
    - [Other Useful Commands](#other-useful-commands)
  - [License](#license)

</details>

## Technology Stack

- **Backend**: Java 21 (using embedded Jetty)
- **Frontend**: HTML + CSS + Node.js + Vite + TypeScript
  - **Public Site**: Focused on mostly static content with minimal JavaScript, optimized for SEO and fast load times.
  - **Dashboard**: React + React Router (soon) + Tailwind CSS (probably)
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
- **Public Site**: General access site with product information and login/registration portal.

## Prerequisites

- **Git** (to clone the repository).
- **Docker v20.10+** and **Docker Compose v2+** (to run the application in a containerized environment).
- **Make** (available by default in most modern Linux distributions).
- **PostgreSQL client** (`psql`) or any GUI (e.g. DBeaver, pgAdmin). Useful for manually inspecting the database.

> [!TIP]
> If you plan to run services directly on your host machine (not via Docker), you’ll also need:
>
> - **Java 21** (for the backend)
> - **Node.js 22+** and a package manager such as `npm`, `pnpm`, or `yarn` (for the frontend)

## Relevant Files

- `Makefile`: Contains commands to simplify developer workflow.
- `compose.yaml`: Defines the common base for all environments.
- `compose.<environment>.yaml`: Defines the specific environment configuration.
- `Dockerfile.<environment>`: Defines how to build each service image for a specific environment.  
  There is also a base `Dockerfile` (without environment suffix) shared by all frontend services and both environments.
- `.env.<environment>.example`: Template files for environment variables used across backend and frontend services.

## Running the Application

### Commands and Workflow Management

The Makefile provides a unified interface to manage the application's environments, services, lifecycle, images, and Docker resources.
All commands are well documented in the `Makefile` itself. You can view them by running:

```bash
make help
# Or simply:
make
```

### How to Run

These steps will guide you through the most basic workflow:

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
make set-env target=<environment> # e.g. development, production

# For example, to set the environment to 'production':
make set-env target=production
```

2. Deploy the application:

```bash
make deploy
```

This will internally execute different commands depending on the selected environment:

- Development: `make build-all nocache=true` → `make up`
- Production: `make pull-all` → `make up`

> [!NOTE]
> If a required `.env.<environment>` file does not exist, it will be created from its `.example` template. You’ll be prompted to edit it manually before proceeding. Currently, the default editor is `nano`.

3. Stop the application

```bash
make down
```

### Accessing the Application

After deploy, you can access the application services through the following URLs (using default ports).

| Environment | Service             | URL                          |
| ----------- | ------------------- | ---------------------------- |
| Development | Backend API         | http://localhost:4001/health |
| Development | Public Site         | http://localhost:5173/       |
| Development | Dashboard           | http://localhost:5174/       |
| Production  | Backend API         | http://localhost:4002/health |
| Production  | Backend (via Nginx) | http://localhost/api/health  |
| Production  | Frontend (Nginx)    | http://localhost/            |

> [!NOTE]
> In production, the application is served via an Nginx reverse proxy which exposes the frontend (dashboard/public-site) on standard HTTP port **80**.

### Other Useful Commands

Besides the basic commands used above, the `Makefile` provides several others with various functionalities. Some of the most useful ones include:

- `make logs target=<service>`: View the logs of the specified running service.
- `make rebuild target=<service>`: Rebuild the specified service image.
- `make restart target=<container>`: Restart the specified running container.
- `make list target=<resource>`: List specific Docker resources. Use `make list-all` to list everything.
- `make prune target=<resource>`: Remove unused Docker resources. Use `make prune-all` to prune all unused resources. Use with **caution**, as this will remove all stopped containers and unreferenced images even if they are unrelated to this application.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for more details.
