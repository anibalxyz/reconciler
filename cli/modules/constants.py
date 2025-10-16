from pathlib import Path
from typing import List, Dict

# Available Services
# Maps service names to their corresponding paths in the project.
SERVICES: Dict[str, str] = {
    "API": "backend/api",
    "DASHBOARD": "frontend/dashboard",
    "FRONTEND": "frontend",
    "PUBLIC_SITE": "frontend/public-site",
    "DB": "db",
    "FLYWAY": "flyway",
    "NGINX": "nginx",
}

# A list of all available environments for the project.
AVAILABLE_ENVS: List[str] = ["dev", "prod", "test"]
# The default environment to use when none is specified.
DEFAULT_ENV: str = AVAILABLE_ENVS[0]

# A list of directories that contain environment-specific `.env` files.
ENV_DIRS: List[Path] = [Path("backend"), Path("frontend")]
# A dictionary that maps each environment to a list of its corresponding `.env` file paths.
ENV_FILES: Dict[str, List[Path]] = {
    str(env): [Path(d, f".env.{env}") for d in ENV_DIRS] for env in AVAILABLE_ENVS
}
