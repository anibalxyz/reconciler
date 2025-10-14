from typing import List, Dict

import typer

from cli.config import SERVICES

app = typer.Typer(help="Manage docker images", no_args_is_help=True)

core_buildable_services: List[str] = [
    SERVICES["API"],
    SERVICES["FRONTEND"],
    SERVICES["PUBLIC_SITE"],
    SERVICES["DASHBOARD"],
]

BUILDABLE_SERVICES: Dict[str, List[str]] = {
    "dev": core_buildable_services,
    "prod": core_buildable_services.append(SERVICES["NGINX"]),
    "test": [SERVICES["API"]],
}

# Services allowed to be pushed to or pulled from a registry
REGISTRY_SERVICES = [SERVICES["API"], SERVICES["NGINX"]]


@app.command()
def build():
    return


@app.command()
def push():
    return


@app.command()
def pull():
    return
