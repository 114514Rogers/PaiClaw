package com.iflytek.astron.workflow.engine.integration.plugins.aitools;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Link {
    private static final Map<String, String> CONST_HEADERS = new HashMap<>();

    static {
        CONST_HEADERS.put("Content-Type", "application/json");
    }

    private final String appId;
    private final List<String> toolIds;
    private final String getUrl;
    private final String runUrl;
    private final String version;
    private final OkHttpClient httpClient;
    private List<HashMap> openApiSchemaList;
    private List<Tool> tools;

    public Link(String appId, List<String> toolIds, String getUrl, String runUrl, String version) {
        this.appId = appId;
        this.toolIds = toolIds;
        this.getUrl = getUrl;
        this.runUrl = runUrl;
        this.version = version;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(300, TimeUnit.SECONDS)
                .build();
        // Retrieve OpenAPI schema list from Link system
        this.openApiSchemaList = toolSchemaList();
        this.tools = new ArrayList<>();
        // Parse schemas and create Tool instances
        parseReactSchemaList();
    }

    public List<Tool> getTools() {
        return tools;
    }

    /**
     * Query tool schema list from Link subsystem.
     */
    private List<HashMap> toolSchemaList() {
        // Build query parameters
        HttpUrl.Builder urlBuilder = HttpUrl.parse(getUrl).newBuilder();
        // Add tool_ids as repeated parameters
        for (String toolId : toolIds) {
            urlBuilder.addQueryParameter("tool_ids", toolId);
        }
        urlBuilder.addQueryParameter("versions", version);
        urlBuilder.addQueryParameter("app_id", appId);

        // Build headers
        Headers headers = Headers.of(CONST_HEADERS);

        Request request = new Request.Builder()
                .url(urlBuilder.build())
                .headers(headers)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                JSONObject jsonResponse = JSON.parseObject(responseBody);
                int code = jsonResponse.getIntValue("code");
                if (code == 0) {
                    JSONObject data = jsonResponse.getJSONObject("data");
                    if (data != null && data.containsKey("tools")) {
                        return data.getJSONArray("tools").toList(HashMap.class);
                    }
                } else {
                    log.error("Failed to get tool schema list, code: {}, message: {}", code, jsonResponse.getString("message"));
                }
            } else {
                log.error("Failed to get tool schema list, response code: {}", response.code());
            }
        } catch (IOException e) {
            log.error("Failed to get tool schema list", e);
        } catch (Exception e) {
            log.error("Unexpected error when getting tool schema list", e);
        }
        return new ArrayList<>();
    }

    /**
     * Parse query parameters from OpenAPI schema.
     */
    private Map<String, Object> parseRequestQuerySchema(List<Map<String, Object>> querySchema) {
        Map<String, Object> queryParameters = new HashMap<>();
        Set<String> queryRequired = new HashSet<>();

        if (querySchema != null) {
            for (Map<String, Object> parameter : querySchema) {
                String parameterName = (String) parameter.get("name");
                String parameterDescription = (String) parameter.get("description");
                @SuppressWarnings("unchecked")
                Map<String, Object> schema = (Map<String, Object>) parameter.get("schema");
                String parameterType = schema != null ? (String) schema.get("type") : null;
                String parameterIn = (String) parameter.get("in");
                Boolean parameterRequired = (Boolean) parameter.get("required");

                if ("query".equals(parameterIn)) {
                    Map<String, Object> paramInfo = new HashMap<>();
                    paramInfo.put("description", parameterDescription);
                    paramInfo.put("type", parameterType);
                    queryParameters.put(parameterName, paramInfo);
                    if (parameterRequired != null && parameterRequired) {
                        queryRequired.add(parameterName);
                    }
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("parameters", queryParameters);
        result.put("required", queryRequired);
        return result;
    }

    /**
     * Recursively parse request body schema.
     */
    private void recursiveParseRequestBodySchema(Map<String, Object> bodySchema,
                                                 Map<String, Object> properties,
                                                 Set<String> requiredSet) {
        if (bodySchema == null) {
            return;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> requestBodyProperties = (Map<String, Object>) bodySchema.get("properties");
        if (requestBodyProperties != null) {
            for (Map.Entry<String, Object> entry : requestBodyProperties.entrySet()) {
                String parameterName = entry.getKey();
                @SuppressWarnings("unchecked")
                Map<String, Object> parameterDetail = (Map<String, Object>) entry.getValue();

                String parameterDescription = (String) parameterDetail.get("description");
                String parameterType = (String) parameterDetail.get("type");

                if ("object".equals(parameterType)) {
                    // Recursively process nested objects
                    Map<String, Object> nestedProperties = new HashMap<>();
                    Set<String> nestedRequired = new HashSet<>();
                    recursiveParseRequestBodySchema(parameterDetail, nestedProperties, nestedRequired);
                    // Merge nested properties
                    properties.putAll(nestedProperties);
                } else {
                    Map<String, Object> paramInfo = new HashMap<>();
                    paramInfo.put("description", parameterDescription);
                    paramInfo.put("type", parameterType);
                    properties.put(parameterName, paramInfo);
                }
            }
        }

        // Add top-level required fields
        @SuppressWarnings("unchecked")
        List<String> requestBodyRequired = (List<String>) bodySchema.get("required");
        if (requestBodyRequired != null) {
            requiredSet.addAll(requestBodyRequired);
        }
    }

    /**
     * Parse OpenAPI schemas and generate Tool instances for ReAct framework.
     */
    private void parseReactSchemaList() {
        if (openApiSchemaList == null) {
            log.warn("OpenAPI schema list is null");
            return;
        }

        for (Map<String, Object> toolSchema : openApiSchemaList) {
            String toolId = (String) toolSchema.get("id");
            if (toolId == null) {
                log.error("Tool ID is empty: {}", JSON.toJSONString(toolSchema));
                continue;
            }

            String schemaStr = (String) toolSchema.get("schema");
            if (schemaStr == null) {
                log.warn("Schema is null for tool: {}", toolId);
                continue;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> schema = JSON.parseObject(schemaStr, Map.class);

            // Process each path and method in the OpenAPI schema
            @SuppressWarnings("unchecked")
            Map<String, Object> paths = (Map<String, Object>) schema.get("paths");
            if (paths != null) {
                for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
                    String path = pathEntry.getKey();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> pathSchema = (Map<String, Object>) pathEntry.getValue();

                    for (Map.Entry<String, Object> methodEntry : pathSchema.entrySet()) {
                        String method = methodEntry.getKey();
                        @SuppressWarnings("unchecked")
                        Map<String, Object> methodSchema = (Map<String, Object>) methodEntry.getValue();

                        String actionName = (String) methodSchema.get("operationId");  // Tool operation name
                        if (actionName == null) {
                            log.warn("OperationId is null for tool: {}, path: {}, method: {}", toolId, path, method);
                            continue;
                        }

                        // Parse query parameters
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> querySchema = (List<Map<String, Object>>) methodSchema.get("parameters");
                        Map<String, Object> queryResult = parseRequestQuerySchema(querySchema);
                        @SuppressWarnings("unchecked")
                        Map<String, Object> queryParameters = (Map<String, Object>) queryResult.get("parameters");

                        // Parse request body (currently only supports application/json format)
                        Map<String, Object> requestBodySchema = getNestedMap(methodSchema, "requestBody", "content", "application/json", "schema");
                        Map<String, Object> bodyParameters = new HashMap<>();
                        Set<String> bodyRequired = new HashSet<>();
                        recursiveParseRequestBodySchema(requestBodySchema, bodyParameters, bodyRequired);

                        // Merge body and query parameters
                        Map<String, Object> parameters = new HashMap<>(queryParameters);
                        parameters.putAll(bodyParameters);

                        // Create Tool execution instance
                        Tool tool = new Tool(
                                appId,
                                toolId,
                                actionName,
                                methodSchema,
                                parameters,
                                getUrl,
                                runUrl,
                                version
                        );
                        tools.add(tool);
                    }
                }
            }
        }
    }

    /**
     * Helper method to get nested map value
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> getNestedMap(Map<String, Object> map, String... keys) {
        if (map == null || keys.length == 0) {
            return new HashMap<>();
        }

        Object current = map;
        for (int i = 0; i < keys.length; i++) {
            String key = keys[i];
            if (current instanceof Map) {
                Map<String, Object> currentMap = (Map<String, Object>) current;
                current = currentMap.get(key);
                if (current == null) {
                    return new HashMap<>();
                }
            } else {
                return new HashMap<>();
            }
        }

        if (current instanceof Map) {
            return (Map<String, Object>) current;
        }
        return new HashMap<>();
    }
}