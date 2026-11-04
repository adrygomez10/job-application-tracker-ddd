"""Check the working tree or an isolated export of Git's staged content."""

import argparse
import shutil
import subprocess
import sys
import tempfile
import uuid
from collections.abc import Sequence
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def run(arguments: Sequence[str], directory: Path) -> int:
    """Run a tool without a shell, preserving its output and exit status."""
    return subprocess.run(
        list(arguments), cwd=directory, check=False
    ).returncode


def tool_python() -> str:
    """Use the development environment installed beside the hook."""
    for relative in ("Scripts/python.exe", "bin/python"):
        candidate = ROOT / ".venv-style" / relative
        if candidate.is_file():
            return str(candidate)
    return sys.executable


def check_tools(directory: Path) -> int:
    """Lint, format-check and strictly type-check this Python adapter."""
    python = tool_python()
    commands = (
        ("ruff", "check", "scripts/check_project.py"),
        ("ruff", "format", "--check", "scripts/check_project.py"),
        ("mypy", "--strict", "scripts/check_project.py"),
    )
    for command in commands:
        result = run((python, "-m", *command), directory)
        if result:
            return result
    return 0


def check_project(directory: Path) -> int:
    """Run the same Java gate for precommit, local work and CI."""
    result = check_tools(directory)
    if result:
        return result

    project = "tracker-ddd-check-" + uuid.uuid4().hex[:10]
    compose = (
        "docker",
        "compose",
        "--project-name",
        project,
        "-f",
        "compose.test.yaml",
    )
    try:
        result = run(
            (
                *compose,
                "run",
                "--rm",
                "--build",
                "--entrypoint",
                "bash",
                "tests",
                "scripts/container-check.sh",
            ),
            directory,
        )
    finally:
        cleanup = run(
            (*compose, "down", "--volumes", "--remove-orphans"),
            directory,
        )
    return result or cleanup


def check_staged() -> int:
    """Export the index; never stage, stash or overwrite working files."""
    with tempfile.TemporaryDirectory(prefix="tracker-ddd-staged-") as name:
        snapshot = Path(name)
        result = run(
            (
                "git",
                "checkout-index",
                "--all",
                "--prefix=" + snapshot.as_posix() + "/",
            ),
            ROOT,
        )
        if result:
            return result

        result = check_project(snapshot)
        reports = snapshot / "target"
        if reports.is_dir():
            shutil.copytree(
                reports, ROOT / "target/precommit", dirs_exist_ok=True
            )
        return result


def main() -> int:
    """Choose explicitly between staged content and the working tree."""
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--staged", action="store_true")
    args = parser.parse_args()
    return check_staged() if args.staged else check_project(ROOT)


if __name__ == "__main__":
    sys.exit(main())
