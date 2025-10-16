import os
import shutil
import subprocess
from typing import Annotated, List, Dict

import typer

from cli.modules.config import get_current_env, set_env
from cli.modules.constants import SERVICES, ENV_FILES
from cli.modules.image import build, get_buildable_services

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
    """
    Validates that all required environment files for the current environment exist.

    If a file does not exist, it is created from its example file.
    The user is then prompted to fill in the missing values.
    """
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


def compose(cmd: List[str]):
    """
    Builds and executes a docker-compose command.

    Args:
        cmd: The docker-compose command to execute (e.g., ["up", "-d"]).
    """
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
    """
    Returns a list of all services that can be managed by docker-compose in the current environment.
    """
    result: List[str] = LIFECYCLE_SERVICES.get(get_current_env()).copy()
    result.append("all")
    for i in range(len(result)):
        result[i] = result[i].split("/")[-1]
    return result


def run_lifecycle_command(command: List[str], services: List[str]):
    """
    Runs a docker-compose lifecycle command for a list of services.

    Args:
        command: The command to run (e.g., ["up"]).
        services: A list of services to run the command on.

    Raises:
        typer.BadParameter: If any of the services are invalid.
    """
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
    help="Manage Docker Compose lifecycle",
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
    detach: Annotated[
        bool,
        typer.Option(help="Enables the use of detach mode."),
    ] = True,
):
    """Brings up containers, networks, and volumes."""
    cmd = ["up"]
    if detach:
        cmd.append("--detach")
    run_lifecycle_command(cmd, services)


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


@app.command()
def rebuild(
    services: Annotated[
        List[str],
        typer.Argument(
            help="Service(s) to rebuild. Accepts multiple values.",
            autocompletion=get_buildable_services,
        ),
    ],
    cache: Annotated[
        bool,
        typer.Option(help="Enables the use of cache during build."),
    ] = True,
):
    """
    Rebuilds and restarts the specified services.

    This command performs the following steps:\n
    1. Takes down the specified services if they are running\n
    2. Builds the Docker images for the specified services\n
    3. Brings the services back up\n
    """
    # Filter the list to get only the services that can be managed by docker-compose.
    runnable_services = [s for s in services if s in get_lifecycle_services()]

    print(f"▶  Rebuilding service(s): {', '.join(services)}")

    print("\n-- Step 1: Taking down services (if running) --")
    if runnable_services:
        down(services=runnable_services)
    else:
        print("INFO: No runnable services found to take down. Skipping.")

    cache_status = "enabled" if cache else "disabled"
    print(f"\n-- Step 2: Building images with cache {cache_status} --")
    build(services=services, cache=cache)

    print("\n-- Step 3: Bringing services up --")
    if runnable_services:
        up(services=runnable_services)
    else:
        print("No runnable services to bring up. Skipping...")


# TODO: add global config for "secure" execution (enabled for pipelines)
@app.command()
def test(
    cache: Annotated[
        bool,
        typer.Option(help="Enables the use of cache during build."),
    ] = True,
):
    """
    Runs the test suite.

    This command performs the following steps:\n
    1. Switches to the test environment\n
    2. Builds all buildable services\n
    3. Brings up the database and Flyway services\n
    4. Brings up the API service and runs the tests\n
    5. Tears down all services\n
    6. Switches back to the original environment\n
    """
    env_snap = get_current_env()
    try:
        set_env("test")

        buildable = get_buildable_services()
        buildable.remove("all")
        build(buildable, cache)

        up(["db", "flyway"])
        up(["api"], False)
    finally:
        try:
            lifecycle = get_lifecycle_services()
            lifecycle.remove("all")
            down(lifecycle)
        finally:
            set_env(env_snap)
