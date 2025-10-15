import os
import shutil
import subprocess
from typing import Annotated, List, Dict

import typer

from cli.modules.config import get_current_env
from cli.modules.constants import SERVICES, ENV_FILES

core_lifecycle_services = [SERVICES["DB"], SERVICES["FLYWAY"]]

LIFECYCLE_SERVICES: Dict[str, List[str]] = {
    "dev": core_lifecycle_services
    + [
        SERVICES["API"],
        SERVICES["PUBLIC_SITE"],
        SERVICES["DASHBOARD"],
    ],
    "prod": core_lifecycle_services + [SERVICES["NGINX"]],
    "test": core_lifecycle_services + [SERVICES["API"]],
}


def validate_env():
    env: str = get_current_env()
    for f in ENV_FILES.get(env):
        if not f.exists():
            print(f"WARNING: Environment file {f} does not exist")
            example = f.with_name(f.name + ".example")
            shutil.copy(example, f)
            print(f"INFO: Created it from example. Please fill it before continuing...")
            if typer.confirm("Do you want to open it now with nano editor?"):
                subprocess.run(["nano", f])
            else:
                print(
                    "Execution aborted. Please complete the file manually and run again."
                )
                raise typer.Exit(1)


# build the compose final command and execute it
def compose(cmd: List[str]):
    env = get_current_env()
    compose_files = ["-f", "compose.yaml", "-f", f"compose.{env}.yaml"]
    env_files = []
    for f in ENV_FILES.get(env):
        env_files += ["--env-file", str(f)]
    project_name = [
        "--project-name",
        f"reconciler-{env}",
    ]  # allows for running multiple env simultaneously

    base_cmd = ["docker", "compose"] + compose_files + env_files + project_name

    run_env = os.environ.copy()
    run_env["APP_ENV"] = env
    subprocess.run(base_cmd + cmd, env=run_env)


def get_lifecycle_services():
    result: List[str] = LIFECYCLE_SERVICES.get(get_current_env()).copy()
    result.append("all")
    for i in range(len(result)):
        result[i] = result[i].split("/")[-1]
    return result


def run_lifecycle_command(command: List[str], services: List[str]):
    services = services or ["all"]
    valid_services = get_lifecycle_services()

    invalid = [s for s in services if s not in valid_services]

    if invalid:
        raise typer.BadParameter(f"Invalid service(s): {', '.join(invalid)}")

    if "all" in services:
        compose(command)
        return

    compose(command + services)


app = typer.Typer(
    help="Manage docker-compose lifecycle (up, down, logs, etc.)",
    no_args_is_help=True,
    callback=validate_env,
)


@app.command()
def up(
    services: Annotated[
        List[str],
        typer.Argument(
            help="Service(s) to bring up. Accepts multiple values.",
            autocompletion=get_lifecycle_services,
        ),
    ] = None,
):
    """Brings up containers, networks, and volumes."""
    run_lifecycle_command(["up", "--detach"], services)


@app.command()
def down(
    services: Annotated[
        List[str],
        typer.Argument(
            help="Service(s) to take down. Accepts multiple values.",
            autocompletion=get_lifecycle_services,
        ),
    ] = None,
):
    """Stops and removes containers, networks, and volumes."""
    run_lifecycle_command(["down", "--remove-orphans"], services)


# TODO: make start/stop/restart/down work over running services only
@app.command()
def start(
    services: Annotated[
        List[str],
        typer.Argument(
            help="Service(s) to start. Accepts multiple values.",
            autocompletion=get_lifecycle_services,
        ),
    ] = None,
):
    """Starts existing, stopped containers."""
    run_lifecycle_command(["start"], services)


@app.command()
def stop(
    services: Annotated[
        List[str],
        typer.Argument(
            help="Service(s) to stop. Accepts multiple values.",
            autocompletion=get_lifecycle_services,
        ),
    ] = None,
):
    """Stops running containers without removing them."""
    run_lifecycle_command(["stop"], services)


@app.command()
def restart(
    services: Annotated[
        List[str],
        typer.Argument(
            help="Service(s) to restart. Accepts multiple values.",
            autocompletion=get_lifecycle_services,
        ),
    ] = None,
):
    """Restarts running containers."""
    run_lifecycle_command(["restart"], services)


@app.command()
def logs(
    services: Annotated[
        List[str],
        typer.Argument(
            help="Service(s) to show logs for. Accepts multiple values.",
            autocompletion=get_lifecycle_services,
        ),
    ] = None,
):
    """Follows log output for services."""
    run_lifecycle_command(["logs", "--follow", "--tail=50"], services)
