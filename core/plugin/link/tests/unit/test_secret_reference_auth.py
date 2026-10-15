from plugin.link.service.community.tools.http import execution_server


class _Redis:
    def get(self, key):
        assert key == "spark_bot:tool_config:tool@tts:V1.0"
        return {
            "authentication": {
                "apiKeySecretRef": {
                    "parameterName": "Authorization",
                    "secretRef": "env:PAIFLOW_ALI_TTS_API_KEY",
                }
            }
        }


def test_process_authentication_resolves_reference_at_execution_time(monkeypatch):
    monkeypatch.setenv("PAIFLOW_ALI_TTS_API_KEY", "Bearer real-secret")
    monkeypatch.setattr(execution_server, "get_redis_engine", lambda: _Redis())
    headers = {}
    query = {}
    schema = {
        "security_type": "Authorization",
        "security": {
            "Authorization": {
                "type": "apiKey",
                "in": "header",
            }
        },
    }

    execution_server.process_authentication(
        schema,
        headers,
        query,
        "tool@tts",
        "V1.0",
    )

    assert headers == {"Authorization": "Bearer real-secret"}
    assert query == {}
