from pathlib import Path
from typing import List, Dict
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


AVAILABLE_ENVS: List[str] = ["dev", "prod", "test"]
DEFAULT_ENV: str = AVAILABLE_ENVS[0]

ENV_DIRS: List[Path] = [Path("backend"), Path("frontend")]
ENV_FILES: Dict[str, List[Path]] = {
    str(env): [Path(d, f".env.{env}") for d in ENV_DIRS] for env in AVAILABLE_ENVS
}