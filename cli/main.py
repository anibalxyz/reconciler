import os
from pathlib import Path

import typer

from cli.modules import compose, image, resource, config

os.chdir(Path(__file__).resolve().parent.parent)

app = typer.Typer(help="CLI for managing the Reconciler project", no_args_is_help=True)

app.add_typer(compose.app, name="compose")
app.add_typer(image.app, name="image")
app.add_typer(resource.app, name="resource")
app.add_typer(config.set_app, name="set")
app.add_typer(config.get_app, name="get")
