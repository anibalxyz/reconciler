import os
from pathlib import Path
from typing import Annotated

import typer

from cli import config as cfg
from cli.config import DOCKER_RESOURCES

os.chdir(Path(__file__).resolve().parent.parent)

app = typer.Typer(help="CLI for managing the Reconciler project", no_args_is_help=True)


@app.command()
def get_env():
    """Gets the current environment."""
    print(f"▶  Current environment: '{cfg.get_config_env()}'")


@app.command()
def set_env(
        env: Annotated[
            str,
            typer.Argument(
                help="The environment variable to set",
                autocompletion=lambda: cfg.AVAILABLE_ENVS,
            ),
        ],
):
    """Sets the current environment."""
    if not env in cfg.AVAILABLE_ENVS:
        print(f"ERROR: Invalid environment: {env}")
        print(f"Available environments: {cfg.AVAILABLE_ENVS}")
        raise typer.Exit(code=1)

    cfg.set_config_env(env)
    print(f"▶  Environment set to: '{env}'")


@app.command()
def build():
    return


@app.command()
def push():
    return


@app.command()
def pull():
    return


@app.command()
def up():
    return


@app.command()
def down():
    return


@app.command()
def start():
    return


@app.command()
def stop():
    return


@app.command()
def restart():
    return


@app.command()
def logs():
    return


@app.command()
def prune():
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
        for r in cfg.DOCKER_RESOURCES:
            cfg.list_resources(r)
            if r != DOCKER_RESOURCES[-1]:
                print()
    else:
        cfg.list_resources(rss)
    return
