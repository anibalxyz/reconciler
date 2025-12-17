import shutil
import subprocess
from pathlib import Path
from typing import List, Annotated

import typer
from typer import Context

from modules.constants import AVAILABLE_ENVS, DEFAULT_ENV, ENV_FILES

CONFIG_FILE_PATH = Path("cli.cfg")


def get_config_value(key: str) -> str | None:
    """
    Retrieves a configuration value for a given key from the configuration file.

    Args:
        key: The configuration key to look for.

    Returns:
        The value of the configuration key, or None if not found.
    """
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
    """
    Sets a configuration value for a given key in the configuration file.

    If the configuration file does not exist, it will be created.
    If the key does not exist, it will be added to the file.
    If the key already exists, its value will be updated.

    Args:
        key: The configuration key to set.
        value: The value to set for the key.

    Returns:
        A message indicating the result of the operation.
    """
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
    """
    Retrieves the current environment from the configuration file.

    If the configuration file or the ENV key is not found, it sets the environment to the default value.
    If the environment is invalid, it resets it to the default value.

    Returns:
        The current environment.
    """
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


def validate_env_callback(ctx: Context):
    """
    [!TEMPORAL] Typer callback to validate environment before running a compose command.
    Skips validation for the 'test' command, as it handles its own validation.
    """
    if ctx.invoked_subcommand == "test":
        return
    validate_env()


set_app = typer.Typer(
    help="Set a configuration value", name="set", no_args_is_help=True
)
get_app = typer.Typer(
    help="Get a configuration value", name="get", no_args_is_help=True
)


@get_app.command(name="env")
def get_env():
    """
    Gets the current environment from the configuration file.
    """
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
    init: Annotated[
        bool, typer.Option(help="Generate required env files for the given environment")
    ] = False,
):
    """
    Sets the current environment in the configuration file.
    """
    if not env in AVAILABLE_ENVS:
        print(f"ERROR: Invalid environment: {env}")
        print(f"Available environments: {AVAILABLE_ENVS}")
        raise typer.Exit(code=1)

    output_msg = set_config_value("ENV", env)
    print(output_msg)

    if init:
        validate_env()
