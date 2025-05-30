# -------------------------------------------------------------------------------
# 1) DEFAULT ENVIRONMENT (can be replaced by `make set-env`)
# -------------------------------------------------------------------------------

ENV := development

# -------------------------------------------------------------------------------
# 2) DETERMINE DOCKER COMPOSE FILE AND .env FILE BASED ON "ENV"
# -------------------------------------------------------------------------------

COMPOSE_FILE := compose.$(ENV).yaml

ENV_FILE := ./backend/.env.$(ENV)

COMPOSE_FILES_CHAIN := -f compose.yaml -f $(COMPOSE_FILE) --env-file $(ENV_FILE)

PROJECT_NAME := --project-name reconciler-$(ENV)

COMPOSE_SETUP := $(COMPOSE_FILES_CHAIN) $(PROJECT_NAME) 

COMPOSE := docker compose

# Avoid printing the directory name when running commands
MAKEFLAGS += --no-print-directory


# -------------------------------------------------------------------------------
# 3) TARGET TO CHANGE ENV INSIDE THIS Makefile ITSELF
#    Usage: make set-env NEW_ENV=<environment>
# -------------------------------------------------------------------------------

.PHONY: set-env
set-env:
	@if [ -z "$(NEW_ENV)" ]; then \
		echo "ERROR: You must specify NEW_ENV. Example: 'make set-env NEW_ENV=production'"; \
		exit 1; \
	fi
	@if [ ! -f "./backend/.env.$(NEW_ENV).example" ]; then \
		echo "ERROR: '$(NEW_ENV)' environment does not exist. Please create it or use a valid environment."; \
		exit 1; \
	fi
	@echo "▶  Setting ENV to '$(NEW_ENV)' in Makefile..."
	@sed -i 's/^ENV := .*/ENV := $(NEW_ENV)/' $(MAKEFILE_LIST)
	@echo "✓  ENV is now '$(NEW_ENV)'. Run 'make up' to bring up that environment."


get-env:
	@echo "▶  Current ENV is '$(ENV)'."
	@echo "✓  To change it, use 'make set-env NEW_ENV=<new_env>'."

# -------------------------------------------------------------------------------
# 4) HELP / USAGE (default goal)
# -------------------------------------------------------------------------------

.DEFAULT_GOAL := help

.PHONY: help
help:
	@echo
	@echo "Usage: make <command>"
	@echo
	@echo "Environment:"
	@echo "  set-env NEW_ENV=<env>         Set the active environment in this Makefile"
	@echo "                                (e.g. make set-env NEW_ENV=production)"
	@echo "  get-env                       Show the current environment"
	@echo
	@echo "Life-Cycle:"
	@echo "  up                             Bring up containers for the current ENV"
	@echo "  rebuild [nocache=true]         Rebuild images (use nocache=true to avoid cache) and restart"
	@echo "  down [rmorphans=true]          Stop and remove containers (rmorphans=true removes orphans)"
	@echo "  start                          Start stopped containers"
	@echo "  stop                           Stop running containers"
	@echo "  restart                        Restart running containers"
	@echo
	@echo "Logs & Status:"
	@echo "  logs                           Tail container logs (last 50 lines)"
	@echo "  ps                             List running containers"
	@echo "  ps-all                         List all containers (including stopped)"
	@echo
	@echo "Cleanup:"
	@echo "  prune-images                   Remove unused images"
	@echo "  prune-containers               Remove stopped containers"
	@echo "  prune-volumes                  Remove unused volumes"
	@echo "  prune-networks                 Remove unused networks"
	@echo "  prune-all                      Remove unused images, containers, volumes, and networks"
	@echo
	@echo "Listing Resources:"
	@echo "  list-images                    List all Docker images"
	@echo "  list-containers                List all Docker containers"
	@echo "  list-volumes                   List all Docker volumes"
	@echo "  list-networks                  List all Docker networks"
	@echo "  list-all                       List all images, containers, volumes, and networks"
	@echo 

# -------------------------------------------------------------------------------
# 5) CORE COMMANDS (read $(ENV), $(COMPOSE_FILE), $(ENV_FILE))
# -------------------------------------------------------------------------------

.PHONY: up rebuild down start stop restart

up: check-env
	@echo "▶  Bringing up '$(ENV)' environment..."
	@$(COMPOSE) $(COMPOSE_SETUP) up -d

rebuild: check-env
	@echo "▶  Rebuilding '$(ENV)' environment..."
	@if [ "$(nocache)" = "true" ]; then \
		$(COMPOSE) $(COMPOSE_SETUP) build --no-cache api; \
	else \
		$(COMPOSE) $(COMPOSE_SETUP) build api; \
	fi
	@$(MAKE) up

down: check-env
	@echo "▶  Stopping & removing '$(ENV)' environment..."
	@$(COMPOSE) $(COMPOSE_SETUP) down $(if $(filter true, $(rmorphans)),--remove-orphans)

start: check-env
	@echo "▶  Starting '$(ENV)' environment..."
	@$(COMPOSE) $(COMPOSE_SETUP) start

stop: check-env
	@echo "▶  Stopping '$(ENV)' environment..."
	@$(COMPOSE) $(COMPOSE_SETUP) stop

restart: check-env
	@echo "▶  Restarting '$(ENV)' environment..."
	@$(COMPOSE) $(COMPOSE_SETUP) restart

# -------------------------------------------------------------------------------
# 6) LOGS & STATUS
# -------------------------------------------------------------------------------

.PHONY: logs ps ps-all

logs:
	@echo "▶  Tailing logs (last 50 lines)..."
	@# "|| true" is used to ignore errors from the logs command
	@$(COMPOSE) $(COMPOSE_SETUP) logs -f --tail=50 || true

ps:
	@echo "▶  Listing running containers..."
	@$(COMPOSE) $(COMPOSE_SETUP) ps

ps-all:
	@echo "▶  Listing all containers (including stopped)..."
	@$(COMPOSE) $(COMPOSE_SETUP) ps -a

# -------------------------------------------------------------------------------
# 7) PRUNE / CLEANUP
# -------------------------------------------------------------------------------

.PHONY: prune-images prune-containers prune-volumes prune-networks prune-all

prune-images:
	@echo "▶  Pruning unused images..."
	@docker image prune -f
	@echo

prune-containers:
	@echo "▶  Pruning stopped containers..."
	@docker container prune -f
	@echo

prune-volumes:
	@echo "▶  Pruning unused volumes..."
	@docker volume prune -f
	@echo

prune-networks:
	@echo "▶  Pruning unused networks..."
	@docker network prune -f
	@echo

prune-all:
	@echo
	@$(MAKE) prune-images prune-containers prune-volumes prune-networks

# -------------------------------------------------------------------------------
# 8) LIST RESOURCES
# -------------------------------------------------------------------------------

.PHONY: list-images list-containers list-volumes list-networks list-all

list-images:
	@echo "▶  Listing Docker images..."
	@docker image ls
	@echo

list-containers:
	@echo "▶  Listing all Docker containers..."
	@docker ps -a
	@echo

list-volumes:
	@echo "▶  Listing Docker volumes..."
	@docker volume ls
	@echo

list-networks:
	@echo "▶  Listing Docker networks..."
	@docker network ls
	@echo

list-all:
	@echo
	@$(MAKE) list-images list-containers list-volumes list-networks

# -------------------------------------------------------------------------------
# 9) ENV FILE CHECK
#    If ./backend/.env.$(ENV) is missing, copy from .env.$(ENV).example
# -------------------------------------------------------------------------------

.PHONY: check-env
check-env:
	@if [ ! -f $(ENV_FILE) ]; then \
		echo "--> Missing file: $(ENV_FILE)."; \
		cp $(ENV_FILE).example $(ENV_FILE); \
		echo "--> File '$(ENV_FILE)' created from example. Please fill it before continuing."; \
		read -p "Do you want to open it now with nano editor? (y/N) " RESP; \
		if [ "$$RESP" != "y" ] && [ "$$RESP" != "Y" ]; then \
			echo "Execution aborted. Please complete the file manually and run again."; \
			exit 1; \
		else \
			nano $(ENV_FILE); \
		fi; \
	fi
