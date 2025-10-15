import subprocess
from typing import Annotated, List

import typer

app = typer.Typer(help="Manage docker resources", no_args_is_help=True)


DOCKER_RESOURCES: List[str] = ["images", "containers", "volumes", "networks"]


def list_resources(rss: str):
    if not rss in DOCKER_RESOURCES:
        raise ValueError(f"Invalid resource: {rss}")

    print(f"Listing {rss}...")
    subprocess.run(["docker", rss[:-1], "ls"], check=True)


def prune_resources(rss: str):
    if not rss in DOCKER_RESOURCES:
        raise ValueError(f"Invalid resource: {rss}")

    print(f"Pruning {rss}...")
    subprocess.run(["docker", rss[:-1], "prune", "-f"], check=True)


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
    if rss == "all":
        for r in DOCKER_RESOURCES:
            prune_resources(r)
            if r != DOCKER_RESOURCES[-1]:
                print()
    else:
        prune_resources(rss)
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
    if rss == "all":
        for r in DOCKER_RESOURCES:
            list_resources(r)
            if r != DOCKER_RESOURCES[-1]:
                print()
    else:
        list_resources(rss)
    return
