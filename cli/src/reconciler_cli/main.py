"""
Main CLI for managing the Reconciler project.

This script serves as the entry point for the command-line interface.
It changes the current working directory to the project root to ensure that all paths are resolved correctly.
"""

import os
import shutil
from pathlib import Path

import typer

from reconciler_cli.modules import compose, image, resource, config

def find_project_root():
    """Find project root by searching up to MAX_SEARCH_DEPTH parent directories."""
    MAX_SEARCH_DEPTH = 20
    current = Path.cwd()
    
    for _ in range(MAX_SEARCH_DEPTH):
        if (current / "compose.yaml").exists():
            return current
        if current.parent == current:
            break
        current = current.parent
    
    raise RuntimeError("Not inside reconciler project")

# Change the current working directory to the project root.
# This is necessary so that all file paths can be resolved relative to the project root.
os.chdir(find_project_root())


def check_docker_is_installed():
    """Checks if Docker is installed and exits if it is not."""
    if not shutil.which("docker"):
        print("ERROR: Docker is not installed or not in the system's PATH.")
        raise typer.Exit(1)


check_docker_is_installed()

app = typer.Typer(help="CLI for managing the Reconciler project", no_args_is_help=True)

app.add_typer(compose.app, name="compose")
app.add_typer(image.app, name="image")
app.add_typer(resource.app, name="resource")
app.add_typer(config.set_app, name="set")
app.add_typer(config.get_app, name="get")
