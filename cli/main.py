import os
from pathlib import Path
from typing import Annotated

import typer

from cli import config as cfg
from cli.modules import compose, image, resource

os.chdir(Path(__file__).resolve().parent.parent)

app = typer.Typer(help="CLI for managing the Reconciler project", no_args_is_help=True)

app.add_typer(compose.app, name="compose")
app.add_typer(image.app, name="image")
app.add_typer(resource.app, name="resource")


@app.command()
def get_env():
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
    if not env in cfg.AVAILABLE_ENVS:
        print(f"ERROR: Invalid environment: {env}")
        print(f"Available environments: {cfg.AVAILABLE_ENVS}")
        raise typer.Exit(code=1)

    cfg.set_config_env(env)
    print(f"▶  Environment set to: '{env}'")
