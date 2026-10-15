import os
import re
from pathlib import Path
from typing import Any
from urllib.parse import parse_qsl, urlencode, urlsplit, urlunsplit

from dotenv import dotenv_values

ENV_REFERENCE_PREFIX = "env:"
REDACTED = "[REDACTED]"

_ENV_NAME_PATTERN = re.compile(r"[A-Z][A-Z0-9_]{2,127}")
_SENSITIVE_KEY_PATTERN = re.compile(
    r"authorization|api[-_]?key|token|secret|password|cookie", re.IGNORECASE
)
_LOCAL_SECRETS: dict[str, str] = {}


def normalize_secret_reference(value: str) -> str:
    normalized = value.strip()
    if not normalized.startswith(ENV_REFERENCE_PREFIX):
        normalized = f"{ENV_REFERENCE_PREFIX}{normalized}"
    _variable_name(normalized)
    return normalized


def resolve_secret_reference(value: str) -> str:
    if not value.startswith(ENV_REFERENCE_PREFIX):
        return value
    variable_name = _variable_name(value)
    environment_value = os.getenv(variable_name)
    if environment_value:
        return environment_value
    file_value = _LOCAL_SECRETS.get(variable_name)
    if file_value:
        return file_value
    raise ValueError(f"Secret environment variable is not configured: {variable_name}")


def resolve_secret_values(values: dict[str, Any]) -> dict[str, Any]:
    return {
        key: resolve_secret_reference(value) if isinstance(value, str) else value
        for key, value in values.items()
    }


def redact_sensitive_data(value: Any) -> Any:
    if isinstance(value, dict):
        return {
            key: (
                REDACTED
                if _SENSITIVE_KEY_PATTERN.search(str(key))
                else redact_sensitive_data(item)
            )
            for key, item in value.items()
        }
    if isinstance(value, list):
        return [redact_sensitive_data(item) for item in value]
    return value


def redact_url(url: str) -> str:
    parsed = urlsplit(url)
    query = [
        (key, REDACTED if _SENSITIVE_KEY_PATTERN.search(key) else value)
        for key, value in parse_qsl(parsed.query, keep_blank_values=True)
    ]
    return urlunsplit(
        (parsed.scheme, parsed.netloc, parsed.path, urlencode(query), parsed.fragment)
    )


def _variable_name(reference: str) -> str:
    if not reference.startswith(ENV_REFERENCE_PREFIX):
        raise ValueError("Secret reference must start with env:")
    variable_name = reference[len(ENV_REFERENCE_PREFIX) :]
    if not _ENV_NAME_PATTERN.fullmatch(variable_name):
        raise ValueError("Invalid secret environment variable name")
    return variable_name


def _secret_file_candidates() -> list[Path]:
    candidates: list[Path] = []
    configured = os.getenv("PAIFLOW_SECRETS_FILE")
    if configured:
        candidates.append(Path(configured).expanduser().resolve())
    candidates.append(Path(__file__).resolve().parents[3] / ".env.local")
    current = Path.cwd().resolve()
    for _ in range(8):
        candidates.append(current / ".env.local")
        if current.parent == current:
            break
        current = current.parent
    return list(dict.fromkeys(candidates))


def _load_local_secrets() -> dict[str, str]:
    secrets: dict[str, str] = {}
    for candidate in _secret_file_candidates():
        if not candidate.is_file():
            continue
        for key, value in dotenv_values(candidate).items():
            if key not in secrets and value:
                secrets[key] = value
    return secrets


_LOCAL_SECRETS.update(_load_local_secrets())
