from pathlib import Path
from typing import List, Annotated

import typer

from cli.modules.constants import AVAILABLE_ENVS, DEFAULT_ENV

CONFIG_FILE_PATH = Path("cli.cfg")


def get_config_value(key: str) -> str | None:
    if not CONFIG_FILE_PATH.exists():
        return None

    with open(CONFIG_FILE_PATH, "r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()

            if not line or line.startswith("#"):
                continue

            if line.startswith(f"{key} = ") or line.startswith(f"{key}="):
                return line.split("=", 1)[1].strip().strip('"').strip("'")

    return None


def set_config_value(key: str, value: str) -> str:
    lines: List[str] = []
    file_exists = CONFIG_FILE_PATH.exists()
    if file_exists:
        with open(CONFIG_FILE_PATH, "r", encoding="utf-8") as f:
            lines = f.readlines()

    has_key = False
    for i, line in enumerate(lines):
        if line.strip().startswith(f"{key} =") or line.strip().startswith(f"{key}="):
            lines[i] = f"{key} = {value}\n"
            has_key = True
            break

    output_msg: str
    if not has_key:
        lines.append(f"{key} = {value}\n")
        if file_exists:
            output_msg = f"INFO: '{key}' key not found in '{CONFIG_FILE_PATH}'. Added it with '{value}' value"
        else:
            output_msg = f"INFO: config file '{CONFIG_FILE_PATH}' did not exist. Created it with '{key}' = '{value}'"
    else:
        output_msg = f"INFO: '{key}' key updated to '{value}' in '{CONFIG_FILE_PATH}'"

    with open(CONFIG_FILE_PATH, "w", encoding="utf-8") as f:
        f.writelines(lines)

    return output_msg


def get_current_env() -> str:
    if not CONFIG_FILE_PATH.exists():
        set_config_value("ENV", DEFAULT_ENV)
        return DEFAULT_ENV

    current_env: str = get_config_value("ENV")
    if current_env and (current_env in AVAILABLE_ENVS):
        return current_env

    print(
        f"Warning: Invalid or missing 'ENV' in '{CONFIG_FILE_PATH}'. "
        f"Resetting to default: '{DEFAULT_ENV}'."
    )
    set_config_value("ENV", DEFAULT_ENV)
    return DEFAULT_ENV


set_app = typer.Typer(
    help="Set a configuration value", name="set", no_args_is_help=True
)
get_app = typer.Typer(
    help="Get a configuration value", name="get", no_args_is_help=True
)


@get_app.command(name="env")
def get_env():
    print(f"▶  Current environment: '{get_current_env()}'")


@set_app.command(name="env")
def set_env(
    env: Annotated[
        str,
        typer.Argument(
            help="The environment variable to set",
            autocompletion=lambda: AVAILABLE_ENVS,
        ),
    ],
):
    if not env in AVAILABLE_ENVS:
        print(f"ERROR: Invalid environment: {env}")
        print(f"Available environments: {AVAILABLE_ENVS}")
        raise typer.Exit(code=1)

    output_msg = set_config_value("ENV", env)
    print(output_msg)
