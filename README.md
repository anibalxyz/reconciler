# Reconciler

[🇪🇸 Versión en español](README.es.md)

A transaction reconciliation platform. Built from scratch as a personal project to **learn** modern software engineering and best practices.

<details>
<summary>Table of Contents</summary>

- [Overview](#overview)
  - [Product](#product)
  - [Personal Project](#personal-project)
- [Product Features](#product-features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [License](#license)

</details>

## Overview

### Product

A financial reconciliation platform for matching transactions between bank statements and internal systems. Upload data from both sources, let the system find matches, then review and resolve discrepancies.

This is the project's visible face and what it will ultimately deliver. Think of it as the excuse that justifies building everything below.

### Personal Project

Reconciler started as a simple Java CLI to learn Java syntax (and OOP by force) before starting a larger project I had planned.

But I thought of adding a web UI, then adding Docker to learn it along the way, then implementing things I was learning in parallel such as good practices,
then I realized I could automate the Docker setup with a custom CLI (initially made with GNU Make), and then... here we are: this became 99% of my portfolio,
a production-grade system (Claude said it, so it must be true) that I built from zero to this point.

I'm applying clean architecture with feature-based packaging, comprehensive testing (unit, integration, coverage), Docker with multi-environment Compose, CI/CD through GitHub Actions, observability with Prometheus, Grafana, Loki, and Promtail, and infrastructure management from the CLI to the deployed server.

Of course there is a long way to go (which is getting longer every day I learn something new), but I'm proud of how much I've grown and learned; I feel ready and _confident_ to integrate into a team and contribute from day one.

There is an astonishing and unbelievable amount of _zero_ "Product Features"... but there is a solid enough foundation ready to grow and build something real

## Product Features

**Legend**:

- ✅ **Done** - Feature implemented and functional
- 🔨 **Done+** - Feature implemented with known improvement opportunities
- 🚧 **Doing** - Currently in development
- 📋 **Todo** - Planned for future implementation

---

- 🚧 **Public Site**: General access site with login/registration portal using Astro for SEO-optimized SSR
  - _Current state_: Technical infrastructure complete, temporary placeholder content and navbar
- 🔨 **User Authentication**: JWT-based authentication with refresh token rotation, time-window access control (Mon-Fri 08:00-20:00), and logout functionality
  - _Known improvements_: Additional time-window rules, revoked cookie blocking mechanisms
- 🚧 **Dashboard**: React-based authenticated central hub for navigating application features
  - _Current state_: Authentication flow complete, temporary Swagger UI link placeholder
- 🔨 **User Management**: CRUD operations for user accounts with role-based access control
  - _Known improvements_: Pagination, restrict creation to admins only
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

```properties
.
├── cli/                 # Python CLI tool
├── backend/
│   ├── api/             # Java API (Javalin) source code
│   └── db/
│       └── migrations/  # Database migrations (Flyway)
├── frontend/
│   ├── common/          # Shared utilities and services
│   ├── dashboard/       # React application for the dashboard
│   └── public-site/     # Astro application for the public site
├── nginx/               # Nginx configuration for the production environment
├── monitoring/          # Monitoring stack configuration (Prometheus, Grafana, Loki, Promtail)
├── compose.yaml         # Base Docker Compose configuration for all services
├── compose.<env>.yaml   # Docker Compose overrides for the <env> environment
├── docs/                # Reference documentation (backend, frontend, infra, cli)
├── .agents/skills/      # Procedural conventions for commits, PRs, testing, releases
├── .github/             # Issue & PR templates, CI/CD workflows
├── AGENTS.md            # AI agents guide
├── CONTRIBUTING.md      # Contribution guide
└── README.md            # This file
```

## Getting Started

**Prerequisites:** Git, Docker v20.10+ with Compose v2+ or v5.1+, Python 3.10+.

> [!NOTE]
> Docker is the actively supported way to run the project.
> Some services can also run locally for development, but that requires additional tooling.

```bash
# Clone the repository
git clone git@github.com:anibalxyz/reconciler.git
cd reconciler
```

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

| Service     | URL (default port)       |
| :---------- | :----------------------- |
| API         | <http://localhost:4001/> |
| Public Site | <http://localhost:5174/> |
| Dashboard   | <http://localhost:5175/> |

```bash
# Stop and remove all containers
cli compose down all
```

> [!TIP]
> For the full guide including prerequisites, CLI installation, dev walkthrough, and architecture, see [docs/infra/cli.md](docs/infra/cli.md).
>
> A one-liner demo command with fixed configuration (no setup required) is coming soon.

## Contributing

Interested in contributing? See [CONTRIBUTING.md](./CONTRIBUTING.md).

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for more details.
