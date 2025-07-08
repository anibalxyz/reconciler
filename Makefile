# ==================================================================================================
# 0) Makefile Initial Configuration And Help Command
# ==================================================================================================
.PHONY: help

# Avoid printing the directory name when running commands
MAKEFLAGS += --no-print-directory

.DEFAULT_GOAL := help

help:
	@echo "Usage:"
	@echo "  make <command> [target=<service|resource>] [flag=true|false]"
	@echo
	@echo "Available flags (only for some commands):"
	@echo "  nocache=true                    # Skip cache during build"
	@echo "  keep-orphans=true               # (down) Keep orphan containers"
	@echo "  extra=true                      # (check-service) Allow targeting services only used in"
	@echo "                                    lifecycle commands"
	@echo "  all=true                        # (ps) Show all containers, including stopped"
	@echo
	@echo "Additional parameters:"
	@echo "  command=<push|pull>             # (check-service) Required when running push or pull to"
	@echo "                                    verify allowed services"
	@echo
	@echo "Environment Management:"
	@echo "  set-env        target=<env>     # Set Makefile ENV (development | production)"
	@echo "  get-env                         # Show current ENV"
	@echo "  check-env                       # Verify that required .env files exist"
	@echo
	@echo "Image Build & Registry:"
	@echo "  check-service  target=<svc>     # Validate service for current ENV"
	@echo "  build          target=<svc>     # Build image (Dockerfile or Dockerfile.<env>)"
	@echo "  push           target=<svc>     # Push image (production only)"
	@echo "  pull           target=<svc>     # Pull image (production only)"
	@echo "  rebuild        target=<svc>     # Rebuild image with --no-cache and restart"
	@echo "  build-all                       # Shortcut: build all services"
	@echo "  push-all                        # Shortcut: push  all services"
	@echo "  pull-all                        # Shortcut: pull  all services"
	@echo "  rebuild-all                     # Shortcut: rebuild all services"
	@echo
	@echo "Compose & Lifecycle:"
	@echo "  up             [target=<svc>]   # Start all or one service"
	@echo "  down           [target=<svc>]   # Stop & remove all or one service"
	@echo "                                    (use keep-orphans=true to keep orphans)"
	@echo "  start          [target=<svc>]   # Start stopped containers"
	@echo "  stop           [target=<svc>]   # Stop running containers"
	@echo "  restart        [target=<svc>]   # Restart containers"
	@echo "  logs           [target=<svc>]   # Tail logs (last 50 lines)"
	@echo "  ps             [all=true]       # List running or all containers"
	@echo "  deploy                          # Build/pull and then 'up' for current ENV"	
	@echo
	@echo "Docker Resource Management:"
	@echo "  prune          target=<res>     # Prune images | containers | volumes | networks | all"
	@echo "  list           target=<res>     # List  images | containers | volumes | networks | all"
	@echo "  prune-all                       # Shortcut: prune all resources"
	@echo "  list-all                        # Shortcut: list  all resources"
	@echo


# ==================================================================================================
# 1) Environment Management
# ==================================================================================================
.PHONY: set-env get-env check-env init-env

AVAILABLE_ENVS := development production

MAKE_ENVFILE := .env.make

$(shell if [ ! -f $(MAKE_ENVFILE) ]; then echo 'ENV=development' > $(MAKE_ENVFILE); fi)

ENV := $(shell . ./$(MAKE_ENVFILE) && echo $$ENV)

export APP_ENV := $(ENV)

init-env:
	@if ! grep -q '^ENV=' $(MAKE_ENVFILE) || [ "$(filter $(ENV),$(AVAILABLE_ENVS))" = "" ]; then \
		echo 'ENV=development' > $(MAKE_ENVFILE); \
		echo "ERROR: There was an invalid value or format in ENV value"; \
		echo "→ Changed to default value: 'development'"; \
		echo "→ Available options: $(AVAILABLE_ENVS)"; \
		exit 1; \
	fi

*: init-env

set-env:
	@if [ -z "$(target)" ]; then \
		echo "ERROR: You must specify target. Example: 'make set-env target=production'"; \
		exit 1; \
	fi

	@VALID_ENV=false;\
	for env in $(AVAILABLE_ENVS); do \
		if [ "$(target)" == $$env  ]; then \
			VALID_ENV=true; \
			break; \
		fi; \
	done; \
	if [ "$$VALID_ENV" = false ]; then \
		echo "ERROR: Invalid environment '$(target)'. Available options are: $(AVAILABLE_ENVS)"; \
		exit 1; \
	fi

	@echo "▶  Setting ENV to '$(target)' in '$(MAKE_ENVFILE)' ..."
	@sed -i 's/^ENV=.*/ENV=$(target)/' $(MAKE_ENVFILE)
	@echo "✓  ENV set to '$(target)'. Run 'make up' to start (ensure images exist)."

get-env:
	@echo "▶  Current ENV is '$(ENV)'."
	@echo "✓  To change it, use 'make set-env target=<new_env>'."

check-env:
	@for file in $(ENV_FILES); do \
		if [ ! -f $$file ]; then \
			echo "--> Missing file: $$file."; \
			cp $$file.example $$file; \
			echo "--> File '$$file' created from example. Please fill it before continuing."; \
			read -p "Do you want to open it now with nano editor? (y/N) " RESP; \
			if [ "$$RESP" != "y" ] && [ "$$RESP" != "Y" ]; then \
				echo "Execution aborted. Please complete the file manually and run again."; \
				exit 1; \
			else \
				nano $$file; \
			fi; \
		fi; \
	done

# ==================================================================================================
# 2) Docker Compose & Env File Configuration
#    Builds the -f chain and --env-file flags for `docker compose`
# ==================================================================================================

COMPOSE := docker compose

# Build the environment-specific .env files
ENV_DIRS := backend frontend
ENV_FILES := $(foreach envDir,$(ENV_DIRS),./$(envDir)/.env.$(ENV))
ENV_FILES_CHAIN := $(foreach file,$(ENV_FILES),--env-file $(file))

# Build the compose setup based on the environment
COMPOSE_FILE := compose.$(ENV).yaml
COMPOSE_FILES_CHAIN := -f compose.yaml -f $(COMPOSE_FILE) 
PROJECT_NAME := --project-name reconciler-$(ENV)
COMPOSE_SETUP := $(COMPOSE_FILES_CHAIN) $(ENV_FILES_CHAIN) $(PROJECT_NAME) 

# ==================================================================================================
# 3) Docker Images Management (build, push, pull, rebuild)
# ==================================================================================================
.PHONY: check-service build push pull rebuild build-all push-all pull-all rebuild-all

# Evaluates whether to use the `nocache` option for Docker builds
EVAL_MAKE_NOCACHE := $(if $(filter true, $(nocache)),nocache=true)
EVAL_DOCKER_NOCACHE := $(if $(filter true,$(nocache)),--no-cache)

# Builds the Docker image name based on the target and environment
# Example: target=frontend/dashboard, ENV=production -> anibalxyz/reconciler-production-dashboard
DOCKER_IMAGE := $(if $(target),anibalxyz/reconciler-$(ENV)-$(notdir $(target)))

# Defines the build targets for different environments
CORE_SERVICES := backend/api frontend frontend/dashboard frontend/public-site
DEVELOPMENT_SERVICES := $(CORE_SERVICES)
PRODUCTION_SERVICES := $(CORE_SERVICES) nginx

PUSHABLE_SERVICES := backend/api nginx

# Services used only during lifecycle
EXTRA_SERVICES := db

EVAL_VALID_SERVICES := $($(shell echo $(ENV) | tr a-z A-Z)_SERVICES)

check-service:
	@if [ "$(command)" = "push" ] || [ "$(command)" = "pull" ]; then \
		if ! echo "$(PUSHABLE_SERVICES)" | tr ' ' '\n' | grep -xq "$(target)"; then \
			echo "ERROR: Service '$(target)' is not $(command)able. Allowed: $(PUSHABLE_SERVICES)"; \
			exit 1; \
		fi; \
	fi;

	@valid_services="$(EVAL_VALID_SERVICES) $(if $(extra),$(EXTRA_SERVICES))"; \
	if ! echo "$$valid_services" | tr ' ' '\n' | grep -xq "$(target)"; then \
		echo "ERROR: Invalid service '$(target)'. Available options are: $$valid_services"; \
		exit 1; \
	fi;

build:
	@if [ -z "$(target)" ]; then \
		echo "ERROR: You must specify target=<service> to build."; \
		echo "Example: 'make build target=frontend/dashboard'"; \
		exit 1; \
	fi;

	@if [ "$(target)" = "all" ]; then \
		echo "▶  Building '$(ENV)' environment..."; \
		for service in $(EVAL_VALID_SERVICES); do \
			$(MAKE) build target=$$service $(EVAL_MAKE_NOCACHE); \
		done; \
		exit 0; \
	fi; \
	$(MAKE) check-service || exit 1; \
	echo "▶  Building '$(target)' for '$(ENV)' environment..."; \
	dockerfile_path=$(if $(wildcard ./$(target)/Dockerfile.$(ENV)),./$(target)/Dockerfile.$(ENV)\
	,./$(target)/Dockerfile); \
	docker build $(EVAL_DOCKER_NOCACHE) \
		-f $$dockerfile_path \
		-t $(DOCKER_IMAGE) \
		./$(target)/

push:
	@if [ "$(ENV)" = "development" ]; then \
		echo "ERROR: Pushing images is not allowed in 'development' environment."; \
		exit 1; \
	fi;

	@if [ -z "$(target)" ]; then \
		echo "ERROR: You must specify target=<service> to push."; \
		echo "Example: 'make push target=backend/api'"; \
		exit 1; \
	fi;

	@if [ "$(target)" = "all" ]; then \
		echo "▶  Pushing all images for '$(ENV)' environment..."; \
		for service in $(PUSHABLE_SERVICES); do \
			$(MAKE) push target=$$service; \
		done; \
		exit 0; \
	fi; \
	$(MAKE) check-service command=push || exit 1; \
	echo "▶  Pushing image '$(target)' for '$(ENV)'..."; \
	docker push $(DOCKER_IMAGE);

pull:
	@if [ "$(ENV)" = "development" ]; then \
		echo "ERROR: Pulling images is not allowed in 'development' environment."; \
		exit 1; \
	fi;

	@if [ -z "$(target)" ]; then \
		echo "ERROR: You must specify target=<service> to pull."; \
		echo "Example: 'make pull target=backend/api'"; \
		exit 1; \
	fi;

	@if [ "$(target)" = "all" ]; then \
		echo "▶  Pulling all images for '$(ENV)' environment..."; \
		for service in $(PUSHABLE_SERVICES); do \
			$(MAKE) pull target=$$service; \
		done; \
		exit 0; \
	fi; \
	$(MAKE) check-service command=pull || exit 1; \
	echo "▶  Pulling image '$(target)' for '$(ENV)'..."; \
	docker pull $(DOCKER_IMAGE); \

rebuild:
	@if [ -z "$(target)" ]; then \
		echo "ERROR: You must specify target=<service> to rebuild."; \
		echo "Example: 'make rebuild target=backend/api'"; \
		exit 1; \
	fi;

	@if [ "$(target)" = "all" ]; then \
		echo "▶  Rebuilding all images for '$(ENV)' environment..."; \
	  $(MAKE) down target=; \
	  $(MAKE) build-all nocache=true; \
	  $(MAKE) up target=; \
		exit 0; \
	fi; \
	$(MAKE) check-service || exit 1; \
	echo "▶  Rebuilding '$(target)' for '$(ENV)' environment..."; \
	$(MAKE) down target=$(target); \
	$(MAKE) build target=$(target) nocache=true; \
	$(MAKE) up target=$(target);

build-all:
	@$(MAKE) build target=all $(EVAL_MAKE_NOCACHE)
	
push-all:
	@$(MAKE) push target=all

pull-all:
	@$(MAKE) pull target=all

rebuild-all:
	@$(MAKE) rebuild target=all

# ==================================================================================================
# 4) Project Lifecycle Management
# ==================================================================================================
.PHONY: up down deploy start stop restart logs ps

up: check-env
	@echo "▶  Bringing up$(if $(target), '$(notdir $(target))' service from) \
	'$(ENV)' environment..."
	@if [ -n "$(target)" ]; then \
		$(MAKE) check-service extra=true; \
	fi;
	@$(COMPOSE) $(COMPOSE_SETUP) up $(if $(target),$(notdir $(target))) -d

down: check-env
	@if [ -n "$(target)" ]; then \
		$(MAKE) check-service extra=true; \
	fi;
	@echo "▶  Stopping & removing$(if $(target), '$(notdir $(target))' service from) \
	'$(ENV)' environment..."
	@$(COMPOSE) $(COMPOSE_SETUP) down $(if $(target),$(notdir $(target))) \
	$(if $(filter true,$(keep-orphans)),,--remove-orphans)

deploy:
	@echo "▶  Deploying '$(ENV)' environment..."
	@if [ "$(ENV)" = "development" ]; then \
		$(MAKE) build-all nocache=true; \
	else \
		$(MAKE) pull-all; \
	fi;
	@$(MAKE) up

start: check-env
	@echo "▶  Starting$(if $(target), '$(notdir $(target))' service from) '$(ENV)' environment..."
	@if [ -n "$(target)" ]; then \
		$(MAKE) check-service extra=true; \
	fi;
	@$(COMPOSE) $(COMPOSE_SETUP) start $(if $(target),$(notdir $(target)))

stop: check-env
	@echo "▶  Stopping$(if $(target), '$(notdir $(target))' service from) '$(ENV)' environment..."
	@if [ -n "$(target)" ]; then \
		$(MAKE) check-service extra=true; \
	fi;
	@$(COMPOSE) $(COMPOSE_SETUP) stop $(if $(target),$(notdir $(target)))

restart: check-env
	@echo "▶  Restarting$(if $(target), '$(notdir $(target))' service from) '$(ENV)' environment..."
	@if [ -n "$(target)" ]; then \
		$(MAKE) check-service extra=true; \
	fi;
	@$(COMPOSE) $(COMPOSE_SETUP) restart $(if $(target),$(notdir $(target)))

# In 'development', the 'frontend' service is validated despite lacking a container, but
# Docker handles it correctly. Similarly, 'frontend/<sub-frontend>' behaves the same in 'production'.
logs: check-env
	@if [ -n "$(target)" ]; then \
		$(MAKE) check-service extra=true; \
	fi;
	@echo "▶  Tailing logs for $(if $(target),'$(notdir $(target))' service,all services) \
	(last 50 lines)..."
	@# "|| true" suppress errors from 'logs' command (Ctrl+C does not display an error)
	@$(COMPOSE) $(COMPOSE_SETUP) logs $(if $(target),$(notdir $(target))) -f --tail=50 || true

ps: check-env
	@echo "▶  Listing $(if $(filter true, $(all)),all,running) containers...";
	@$(COMPOSE) $(COMPOSE_SETUP) ps $(if $(filter true, $(all)),-a)

# ==================================================================================================
# 5) Prune / List Docker Resources
# ==================================================================================================
.PHONY: prune list prune-all list-all

RESOURCE_TARGETS := images containers volumes networks

prune:
	@if [ -z "$(target)" ]; then \
		echo "ERROR: You must specify target=<resource> to prune."; \
		echo "Example: 'make prune target=images'"; \
		exit 1; \
	fi;

	@if [ "$(target)" = "all" ]; then \
		for t in $(RESOURCE_TARGETS); do \
			$(MAKE) prune target=$$t; \
		done; \
		exit 0; \
	fi; \
	if ! echo "$(RESOURCE_TARGETS)" | grep -wq "$(target)"; then \
		echo "ERROR: Invalid target '$(target)'. Available options: $(RESOURCE_TARGETS) all"; \
		exit 1; \
	fi; \
	resource=$$(echo $(target) | sed 's/s$$//'); \
	echo "▶  Pruning '$$resource' resources..."; \
	docker $$resource prune -f;

	@if [ "$(target)" != "all" ]; then echo; fi

list:
	@if [ -z "$(target)" ]; then \
		echo "ERROR: You must specify target=<resource> to list."; \
		echo "Example: 'make list target=images'"; \
		exit 1; \
	fi;

	@if [ "$(target)" = "all" ]; then \
		for t in $(RESOURCE_TARGETS); do \
			$(MAKE) list target=$$t; \
		done; \
		exit 0; \
	fi; \
	if ! echo "$(RESOURCE_TARGETS)" | grep -wq "$(target)"; then \
		echo "ERROR: Invalid target '$(target)'. Available options: $(RESOURCE_TARGETS) all"; \
		exit 1; \
	fi; \
	resource=$$(echo $(target) | sed 's/s$$//'); \
	echo "▶  Listing '$$resource' resources..."; \
	docker $$resource ls;

	@if [ "$(target)" != "all" ]; then echo; fi

prune-all:
	@$(MAKE) prune target=all

list-all:
	@$(MAKE) list target=all
