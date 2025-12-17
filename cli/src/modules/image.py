import subprocess
from pathlib import Path
from typing import List, Dict, Annotated

import typer

from modules.config import get_current_env
from modules.constants import SERVICES

app = typer.Typer(help="Manage docker images", no_args_is_help=True)

core_buildable_services: List[str] = [
    SERVICES["API"],
    SERVICES["FRONTEND"],
    SERVICES["PUBLIC_SITE"],
    SERVICES["DASHBOARD"],
]

BUILDABLE_SERVICES: Dict[str, List[str]] = {
    "dev": core_buildable_services,
    "prod": core_buildable_services + [SERVICES["NGINX"]],
    "test": [SERVICES["API"]],
}

# Services allowed to be pushed to or pulled from a registry
REGISTRY_SERVICES = [SERVICES["API"], SERVICES["NGINX"]]


def get_buildable_services():
    """
    Returns a list of all services that can be built in the current environment.
    """
    result: List[str] = BUILDABLE_SERVICES.get(get_current_env()).copy()
    result.append("all")
    for i in range(len(result)):
        result[i] = result[i].split("/")[-1]
    return result


def get_registry_services():
    """
    Returns a list of all services that can be pushed to or pulled from a registry.
    """
    result: List[str] = REGISTRY_SERVICES.copy()
    for i in range(len(result)):
        result[i] = result[i].split("/")[-1]
    return result


def docker(cmd_ref: List[str], service: str):
    """
    Builds and executes a Docker command for a given service.

    Args:
        cmd_ref: The Docker command to execute (e.g., ["build", "--no-cache"]).
        service: The service to run the command on.
    """
    cmd = cmd_ref.copy()
    env = get_current_env()
    image = f"anibalxyz/reconciler-{env}-"
    # .replace() used because of PUBLIC_SITE
    service_with_dir = SERVICES[service.upper().replace("-", "_")]

    base_command = cmd[0]
    if base_command == "build":
        dockerfile_path = Path(f"./{service_with_dir}/Dockerfile.{env}")
        if not dockerfile_path.exists():
            dockerfile_path = dockerfile_path.with_suffix("")

        cmd.append("-f")
        cmd.append(str(dockerfile_path))
        cmd.append("-t")
        cmd.append(f"{image}{service}")
        cmd.append(f"./{service_with_dir}")
    else:
        cmd.append(f"{image}{service}")

    subprocess.run(["docker"] + cmd)


def run_image_command(command: List[str], services: List[str]):
    """
    Runs a Docker image command for a list of services.

    Args:
        command: The Docker command to execute.
        services: A list of services to run the command on.

    Raises:
        typer.BadParameter: If any of the services are invalid.
    """
    valid_services = get_buildable_services().copy()
    invalid = [s for s in services if s not in valid_services]

    if invalid:
        raise typer.BadParameter(f"Invalid service(s): {', '.join(invalid)}")

    if "all" in services:
        valid_services.remove("all")
        for s in valid_services:
            docker(command, s)
        return

    for s in services:
        docker(command, s)


@app.command()
def build(
    services: Annotated[
        List[str],
        typer.Argument(
            help="Service(s) to build. Accepts multiple values.",
            autocompletion=get_buildable_services,
        ),
    ],
    cache: Annotated[
        bool,
        typer.Option(help="Enables the use of cache during build."),
    ] = True,
):
    """
    Builds Docker images for the specified services.
    """
    cmd = ["build"]
    if not cache:
        cmd.append("--no-cache")
    run_image_command(cmd, services)
    return


@app.command()
def push(
    services: Annotated[
        List[str],
        typer.Argument(
            help="Service(s) to push. Accepts multiple values.",
            autocompletion=get_registry_services,
        ),
    ],
):
    """
    Pushes Docker images to a registry.

    This command is only allowed in the production environment.
    """
    if not get_current_env() == "prod":
        print("ERROR: Pushing images is only allowed in production")
        raise typer.Exit(code=1)

    invalid = [s for s in services if s not in get_registry_services()]
    if invalid:
        raise typer.BadParameter(
            f"Service(s) not allowed for registry operations: {', '.join(invalid)}"
        )
    run_image_command(["push"], services)
    return


@app.command()
def pull(
    services: Annotated[
        List[str],
        typer.Argument(
            help="Service(s) to pull. Accepts multiple values.",
            autocompletion=get_registry_services,
        ),
    ],
):
    """
    Pulls Docker images from a registry.

    This command is only allowed in the production environment.
    """
    if not get_current_env() == "prod":
        print("ERROR: Pulling images is only allowed in production")
        raise typer.Exit(code=1)

    invalid = [s for s in services if s not in get_registry_services()]
    if invalid:
        raise typer.BadParameter(
            f"Service(s) not allowed for registry operations: {', '.join(invalid)}"
        )
    run_image_command(["pull"], services)
    return
