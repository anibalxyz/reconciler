from pathlib import Path
from typing import List, Dict

AVAILABLE_ENVS: List[str] = ["dev", "prod", "test"]
DEFAULT_ENV: str = AVAILABLE_ENVS[0]

ENV_DIRS: List[Path] = [Path("backend"), Path("frontend")]
ENV_FILES: Dict[str, List[Path]] = {
    str(env): [Path(d, f".env.{env}") for d in ENV_DIRS] for env in AVAILABLE_ENVS
}


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
