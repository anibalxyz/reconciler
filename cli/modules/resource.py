import subprocess
from typing import Annotated, List

import typer

app = typer.Typer(help="Manage docker resources", no_args_is_help=True)


DOCKER_RESOURCES: List[str] = ["images", "containers", "volumes", "networks"]


def execute_resource_command(verb: str, rss: str):
    """
    Executes a command on a Docker resource.

    Args:
        verb: The command to execute (e.g., "list", "prune").
        rss: The type of resource.

    Raises:
        ValueError: If the resource type or verb is invalid.
    """
    if not rss in DOCKER_RESOURCES:
        raise ValueError(f"Invalid resource: {rss}")

    if verb == "list":
        print(f"Listing {rss}...")
        subprocess.run(["docker", rss[:-1], "ls"])
    elif verb == "prune":
        print(f"Pruning {rss}...")
        subprocess.run(["docker", rss[:-1], "prune", "-f"])
    else:
        raise ValueError(f"Invalid verb: {verb}")


@app.command()
def prune(
    rss: Annotated[
        str,
        typer.Argument(
            help="The resource to be pruned",
            autocompletion=lambda: DOCKER_RESOURCES + ["all"],
        ),
    ],
):
    """
    Prunes Docker resources of a given type, or all resources if "all" is specified.
    """
    if rss == "all":
        for r in DOCKER_RESOURCES:
            execute_resource_command("prune", r)
            if r != DOCKER_RESOURCES[-1]:
                print()
    else:
        execute_resource_command("prune", rss)
    return


@app.command()
def list(
    rss: Annotated[
        str,
        typer.Argument(
            help="The resource to be listed",
            autocompletion=lambda: DOCKER_RESOURCES + ["all"],
        ),
    ] = "all",
):
    """
    Lists Docker resources of a given type, or all resources if "all" is specified.
    """
    if rss == "all":
        for r in DOCKER_RESOURCES:
            execute_resource_command("list", r)
            if r != DOCKER_RESOURCES[-1]:
                print()
    else:
        execute_resource_command("list", rss)
    return
