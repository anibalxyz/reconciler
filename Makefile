# Define the Docker Compose command
COMPOSE=docker compose

# ======================
# Development Commands
# ======================

# 'dev' -> Starts the development environment with Docker Compose.
# Checks if the .env file exists before attempting to bring up the containers.
dev: check-env-dev
	@echo "Starting development with Docker Compose..."
	$(COMPOSE) up -d

# 'dev-rebuild' -> Rebuilds the development containers with Docker Compose.
# Forces a rebuild of the images and restarts the containers.
dev-rebuild: check-env-dev
	@echo "Rebuilding development containers..."
	$(COMPOSE) up -d --build

# 'run' -> Runs the 'reconciler-app' container with Docker Compose.
# The container is removed after execution.
run:
	$(COMPOSE) run --rm reconciler-app

# ======================
# Production Commands
# ======================

# 'prod' -> Starts the production environment with Docker Compose.
# Uses the .env.production file for production configurations.
prod: check-env-prod
	@echo "Starting production with Docker Compose..."
	$(COMPOSE) -f compose.yaml --env-file .env.production up -d

# 'prod-rebuild' -> Rebuilds the production containers with Docker Compose.
# Forces a rebuild of the images and restarts the production containers.
prod-rebuild: check-env-prod
	@echo "Rebuilding production containers..."
	$(COMPOSE) -f compose.yaml --env-file .env.production up -d --build

# 'run-prod' -> Runs the 'reconciler-app' container in the production environment.
# Uses the .env.production file for production configurations.
run-prod:
	$(COMPOSE) -f compose.yaml run --env-from-file .env.production --rm reconciler-app

# ======================
# Utility Commands
# ======================

# 'down' -> Stops and removes the containers, networks, and volumes defined in Compose.
down:
	$(COMPOSE) down

# 'logs' -> Displays the real-time logs of the containers (last 50 entries).
logs:
	$(COMPOSE) logs -f --tail=50

# 'ps' -> Lists the running containers.
ps:
	$(COMPOSE) ps

# 'ps-all' -> Lists all containers, including stopped ones.
ps-all:
	$(COMPOSE) ps -a

# 'clean' -> Stops the containers and removes unused and orphaned volumes.
clean:
	$(COMPOSE) down -v --remove-orphans

# 'prune-all' -> Removes unused images, containers, volumes, and networks.
# This helps free up disk space by cleaning up orphaned resources.
prune-all:
	docker image prune -f
	@echo
	docker container prune -f
	@echo
	docker volume prune -f
	@echo
	docker network prune -f

# 'prune-images' -> Removes unused images.
prune-images:
	docker image prune -f

# 'prune-containers' -> Removes stopped containers.
prune-containers:
	docker container prune -f

# 'prune-volumes' -> Removes unused volumes.
prune-volumes:
	docker volume prune -f

# 'prune-networks' -> Removes unused networks.
prune-networks:
	docker network prune -f

# 'list-all' -> Lists all images, containers, volumes, and networks.
list-all:
	docker image ls
	@echo
	docker ps -a
	@echo
	docker volume ls
	@echo
	docker network ls

# 'list-images' -> Lists all images in the system.
list-images:
	docker image ls

# 'list-containers' -> Lists all containers, including stopped ones.
list-containers:
	docker ps -a

# 'list-volumes' -> Lists all Docker volumes.
list-volumes:
	docker volume ls

# 'list-networks' -> Lists all Docker networks.
list-networks:
	docker network ls

# ======================
# Environment Checks
# ======================

# 'check-env-dev' -> Checks if the .env file exists.
# If not, it creates one from .env.example.
check-env-dev:
	@if [ ! -f .env ]; then \
		echo "No .env file found. Creating one from .env.example..."; \
		cp .env.example .env; \
		sleep 3; \
	fi

# 'check-env-prod' -> Checks if the .env.production file exists.
# If not, it creates one from .env.production.example.
check-env-prod:
	@if [ ! -f .env.production ]; then \
		echo "No .env.production file found. Creating one from .env.production.example..."; \
		cp .env.production.example .env.production; \
		sleep 3; \
	fi

# ======================
# Restart Command
# ======================

# 'restart' -> Restarts the Docker environment based on the provided environment (dev or prod).
# Ensures the proper parameters are passed to select the environment.
restart:
ifndef env
	$(error You must provide an environment: make restart env=dev OR env=prod)
endif
ifeq ($(env),dev)
	$(MAKE) down
	$(MAKE) clean
	$(MAKE) prune-all
	$(MAKE) dev-rebuild
else ifeq ($(env),prod)
	$(MAKE) down
	$(MAKE) clean
	$(MAKE) prune-all
	$(MAKE) prod-rebuild
else
	$(error Invalid env: '$(env)'. Use 'dev' or 'prod')
endif
