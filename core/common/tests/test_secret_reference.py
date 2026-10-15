from common.utils import secret_reference
from common.utils.secret_reference import (
    REDACTED,
    normalize_secret_reference,
    redact_sensitive_data,
    redact_url,
    resolve_secret_reference,
)


def test_secret_reference_resolves_from_environment(monkeypatch):
    monkeypatch.setenv("PAIFLOW_TEST_API_KEY", "secret-value")

    reference = normalize_secret_reference("PAIFLOW_TEST_API_KEY")

    assert reference == "env:PAIFLOW_TEST_API_KEY"
    assert resolve_secret_reference(reference) == "secret-value"


def test_sensitive_values_are_redacted_without_touching_normal_fields():
    payload = {
        "Authorization": "Bearer secret",
        "nested": {"api_key": "secret", "name": "tts"},
    }

    assert redact_sensitive_data(payload) == {
        "Authorization": REDACTED,
        "nested": {"api_key": REDACTED, "name": "tts"},
    }
    assert redact_url("https://example.com/tts?token=secret&voice=longxiaochun") == (
        "https://example.com/tts?token=%5BREDACTED%5D&voice=longxiaochun"
    )


def test_resolution_uses_startup_memory_without_reading_disk(monkeypatch):
    monkeypatch.delenv("PAIFLOW_STARTUP_ONLY_KEY", raising=False)
    monkeypatch.setitem(
        secret_reference._LOCAL_SECRETS,
        "PAIFLOW_STARTUP_ONLY_KEY",
        "startup-value",
    )
    monkeypatch.setattr(
        secret_reference,
        "_secret_file_candidates",
        lambda: (_ for _ in ()).throw(AssertionError("disk must not be read")),
    )

    assert resolve_secret_reference("env:PAIFLOW_STARTUP_ONLY_KEY") == "startup-value"
