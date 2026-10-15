#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
SECRETS_FILE=${PAIFLOW_SECRETS_FILE:-"${ROOT_DIR}/.env.local"}
MYSQL_HOST=${MYSQL_HOST:-localhost}
MYSQL_PORT=${MYSQL_PORT:-3306}
MYSQL_DB=${MYSQL_DB:-paiflow-console}
MYSQL_USER=${MYSQL_USER:-root}
MYSQL_PASSWORD=${MYSQL_PASSWORD:-123456}
REDIS_HOST=${REDIS_HOST:-localhost}
REDIS_PORT=${REDIS_PORT:-6379}
REDIS_PASSWORD=${REDIS_PASSWORD:-}

umask 077
touch "${SECRETS_FILE}"
chmod 600 "${SECRETS_FILE}"

mysql_query() {
    MYSQL_PWD="${MYSQL_PASSWORD}" mysql \
        --batch \
        --raw \
        --skip-column-names \
        --host="${MYSQL_HOST}" \
        --port="${MYSQL_PORT}" \
        --user="${MYSQL_USER}" \
        "${MYSQL_DB}" \
        --execute="$1"
}

redis_command() {
    if [[ -n "${REDIS_PASSWORD}" ]]; then
        REDISCLI_AUTH="${REDIS_PASSWORD}" redis-cli --raw -h "${REDIS_HOST}" -p "${REDIS_PORT}" "$@"
    else
        redis-cli --raw -h "${REDIS_HOST}" -p "${REDIS_PORT}" "$@"
    fi
}

append_secret() {
    local variable_name=$1
    local secret_value=$2
    if ! grep -q "^${variable_name}=" "${SECRETS_FILE}"; then
        if [[ "${secret_value}" == *$'\n'* || "${secret_value}" == *$'\r'* ]]; then
            echo "Refusing to migrate a multiline secret for ${variable_name}" >&2
            exit 1
        fi
        printf '%s=%s\n' "${variable_name}" "${secret_value}" >>"${SECRETS_FILE}"
    fi
}

replace_secret_in_runtime_data() {
    local secret_value=$1
    local reference=$2
    local secret_hex
    local reference_hex
    secret_hex=$(printf '%s' "${secret_value}" | xxd -p -c 1000000)
    reference_hex=$(printf '%s' "${reference}" | xxd -p -c 1000000)
    mysql_query "SET @old=CONVERT(0x${secret_hex} USING utf8mb4), @ref=CONVERT(0x${reference_hex} USING utf8mb4); UPDATE flow_protocol_temp SET biz_protocol=REPLACE(biz_protocol,@old,@ref), sys_protocol=REPLACE(sys_protocol,@old,@ref) WHERE biz_protocol LIKE CONCAT('%',@old,'%') OR sys_protocol LIKE CONCAT('%',@old,'%');" >/dev/null

    if [[ -d "${ROOT_DIR}/.runtime/logs" ]]; then
        OLD_SECRET="${secret_value}" find "${ROOT_DIR}/.runtime/logs" -type f -name '*.log' -exec \
            perl -0pi -e 's/\Q$ENV{OLD_SECRET}\E/[REDACTED]/g' {} +
    fi
}

migrate_redis_secret() {
    local tool_id=$1
    local secret_value=$2
    local reference=$3
    if ! command -v redis-cli >/dev/null 2>&1; then
        return
    fi
    while IFS= read -r redis_key; do
        [[ -n "${redis_key}" ]] || continue
        local cached
        cached=$(redis_command GET "${redis_key}")
        if [[ "${cached}" == *"${secret_value}"* ]]; then
            local migrated
            migrated=$(CACHE_VALUE="${cached}" OLD_SECRET="${secret_value}" SECRET_REFERENCE="${reference}" \
                python3 -c 'import os; print(os.environ["CACHE_VALUE"].replace(os.environ["OLD_SECRET"], os.environ["SECRET_REFERENCE"]))')
            redis_command SET "${redis_key}" "${migrated}" >/dev/null
        fi
    done < <(redis_command --scan --pattern "spark_bot:tool_config:${tool_id}*")
}

model_count=0
while IFS=$'\t' read -r model_id model_name api_key; do
    [[ -n "${model_id}" && -n "${api_key}" ]] || continue
    model_slug=$(printf '%s' "${model_name}" | tr '[:lower:]' '[:upper:]' | sed -E 's/[^A-Z0-9]+/_/g; s/^_+|_+$//g')
    variable_name="PAIFLOW_${model_slug:-MODEL_${model_id}}_API_KEY"
    reference="env:${variable_name}"
    append_secret "${variable_name}" "${api_key}"
    mysql_query "UPDATE model SET api_key='${reference}' WHERE id=${model_id};" >/dev/null
    replace_secret_in_runtime_data "${api_key}" "${reference}"
    model_count=$((model_count + 1))
done < <(mysql_query "SELECT id, name, api_key FROM model WHERE is_deleted=0 AND api_key IS NOT NULL AND api_key<>'' AND api_key NOT LIKE 'env:%';")

tool_count=0
while IFS=$'\t' read -r row_id tool_id tool_name service_token; do
    [[ -n "${row_id}" && -n "${service_token}" ]] || continue
    tool_slug=$(printf '%s' "${tool_name}" | tr '[:lower:]' '[:upper:]' | sed -E 's/[^A-Z0-9]+/_/g; s/^_+|_+$//g')
    variable_name="PAIFLOW_${tool_slug:-TOOL_${row_id}}_AUTH_VALUE"
    reference="env:${variable_name}"
    append_secret "${variable_name}" "${service_token}"
    reference_hex=$(printf '%s' "${reference}" | xxd -p -c 1000000)
    mysql_query "UPDATE tool_box SET auth_info=JSON_REMOVE(JSON_SET(auth_info, '$.secretRef', CONVERT(0x${reference_hex} USING utf8mb4)), '$.serviceToken') WHERE id=${row_id};" >/dev/null
    migrate_redis_secret "${tool_id}" "${service_token}" "${reference}"
    replace_secret_in_runtime_data "${service_token}" "${reference}"
    tool_count=$((tool_count + 1))
done < <(mysql_query "SELECT id, tool_id, name, JSON_UNQUOTE(JSON_EXTRACT(auth_info,'$.serviceToken')) FROM tool_box WHERE deleted=0 AND auth_type=2 AND JSON_VALID(auth_info) AND JSON_EXTRACT(auth_info,'$.serviceToken') IS NOT NULL;")

mysql_query "UPDATE flow_protocol_temp SET biz_protocol=REGEXP_REPLACE(biz_protocol, 'sk-[0-9a-fA-F]{32}', '[REDACTED_LEGACY_API_KEY]'), sys_protocol=REGEXP_REPLACE(sys_protocol, 'sk-[0-9a-fA-F]{32}', '[REDACTED_LEGACY_API_KEY]') WHERE biz_protocol REGEXP 'sk-[0-9a-fA-F]{32}' OR sys_protocol REGEXP 'sk-[0-9a-fA-F]{32}';" >/dev/null

echo "Migrated ${model_count} model secret(s) and ${tool_count} plugin secret(s)."
echo "Secret values are stored only in ${SECRETS_FILE}."
