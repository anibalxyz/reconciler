import subprocess
from pathlib import Path
from typing import List, Dict

AVAILABLE_ENVS: List[str] = ["dev", "prod", "test"]
DEFAULT_ENV: str = AVAILABLE_ENVS[0]

CONFIG_FILE_PATH = Path("cli.cfg")

# Available Services
SERVICES: Dict[str, str] = {
    "API": "backend/api",
    "DASHBOARD": "frontend/dashboard",
    "FRONTEND": "frontend",
    "PUBLIC_SITE": "frontend/public-site",
    "DB": "db",
    "FLYWAY": "flyway",
    "NGINX": "nginx",
}

BUILDABLE_SERVICES: List[str] = [
    SERVICES["API"],
    SERVICES["FRONTEND"],
    SERVICES["PUBLIC_SITE"],
    SERVICES["DASHBOARD"],
]

BUILDABLE_SERVICES_BY_ENV: Dict[str, List[str]] = {
    "dev": BUILDABLE_SERVICES,
    "prod": BUILDABLE_SERVICES + [SERVICES["NGINX"]],
    "test": [SERVICES["API"]],
}

LIFECYCLE_SERVICES = [SERVICES["DB"], SERVICES["FLYWAY"]]

# Services allowed to be pushed to or pulled from a registry
REGISTRY_SERVICES = [SERVICES["API"], SERVICES["NGINX"]]


# build the compose final command and execute it
def compose():
    return


def get_config_value(path: Path, key: str) -> str | None:
    if not path.exists():
        return None

    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()

            if not line or line.startswith("#"):
                continue

            if line.startswith(f"{key} = ") or line.startswith(f"{key}="):
                return line.split("=", 1)[1].strip().strip('"').strip("'")

    return None


def get_config_env() -> str:
    if not CONFIG_FILE_PATH.exists():
        set_config_env(DEFAULT_ENV)
        return DEFAULT_ENV

    current_env: str = get_config_value(CONFIG_FILE_PATH, "ENV")
    if current_env in AVAILABLE_ENVS:
        return current_env

    print(
        f"Warning: Invalid or missing 'ENV' in '{CONFIG_FILE_PATH}'. "
        f"Resetting to default: '{DEFAULT_ENV}'."
    )
    set_config_env(DEFAULT_ENV)
    return DEFAULT_ENV


def set_config_env(env: str):
    if env not in AVAILABLE_ENVS:
        raise ValueError(f"Invalid environment: {env}")

    if not CONFIG_FILE_PATH.exists():
        print(
            f"INFO: config file '{CONFIG_FILE_PATH}' did not exist. Created it with '{env}' value"
        )
        CONFIG_FILE_PATH.write_text(f"ENV = {env}\n")
        return

    lines: List[str]
    with open(CONFIG_FILE_PATH, "r", encoding="utf-8") as f:
        lines = f.readlines()

    has_env_var = False
    for i, line in enumerate(lines):
        if line.strip().startswith("ENV =") or line.strip().startswith("ENV="):
            lines[i] = f"ENV = {env}\n"
            has_env_var = True
            break

    if not has_env_var:
        print(
            f"INFO: 'ENV' key not found in '{CONFIG_FILE_PATH}'. Added it with '{env}' value"
        )
        lines.append(f"ENV = {env}\n")

    with open(CONFIG_FILE_PATH, "w", encoding="utf-8") as f:
        f.writelines(lines)


DOCKER_RESOURCES: List[str] = ["images", "containers", "volumes", "networks"]


def list_resources(rss: str):
    if not rss in DOCKER_RESOURCES:
        raise ValueError(f"Invalid resource: {rss}")

    print(f"Listing {rss}...")
    subprocess.run(["docker", rss[:-1], "ls"], check=True)
