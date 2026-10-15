-- Before running this migration, copy every plaintext value to the local
-- .env.local file. This SQL only removes plaintext from MySQL; it cannot export
-- secret values to a local file.

START TRANSACTION;

UPDATE model
SET api_key = 'env:PAIFLOW_DEEPSEEK_V4_API_KEY'
WHERE is_deleted = 0
  AND name IN ('DeepSeek V4', 'deepseek-chat')
  AND api_key IS NOT NULL
  AND api_key <> ''
  AND api_key NOT LIKE 'env:%';

UPDATE tool_box
SET auth_info = JSON_REMOVE(
        JSON_SET(
            auth_info,
            '$.secretRef',
            CONCAT('env:PAIFLOW_TOOL_', id, '_AUTH_VALUE')
        ),
        '$.serviceToken'
    )
WHERE deleted = 0
  AND auth_type = 2
  AND JSON_VALID(auth_info)
  AND JSON_EXTRACT(auth_info, '$.serviceToken') IS NOT NULL;

UPDATE flow_protocol_temp
SET biz_protocol = REGEXP_REPLACE(
        biz_protocol,
        '"apiKey":"sk-[0-9a-fA-F]{32}"',
        '"apiKey":"env:PAIFLOW_DEEPSEEK_V4_API_KEY"'
    ),
    sys_protocol = REGEXP_REPLACE(
        sys_protocol,
        '"apiKey":"sk-[0-9a-fA-F]{32}"',
        '"apiKey":"env:PAIFLOW_DEEPSEEK_V4_API_KEY"'
    )
WHERE biz_protocol REGEXP '"apiKey":"sk-[0-9a-fA-F]{32}"'
   OR sys_protocol REGEXP '"apiKey":"sk-[0-9a-fA-F]{32}"';

COMMIT;
