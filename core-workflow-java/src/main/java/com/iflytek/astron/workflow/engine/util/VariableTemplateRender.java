package com.iflytek.astron.workflow.engine.util;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 提示词中的变量替换
 *
 * @author YiHui
 * @date 2025/11/28
 */
public class VariableTemplateRender {

    /**
     * Pattern to match variable references: {{node-id.variable-name}}
     */
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{([^}]+)\\}\\}");


    // fixme 需要考虑inputs 的value是json串，替换json串的某一个内部值的场景
    public static String render(String template, Map<String, Object> inputs) {
        if (template == null || template.isEmpty()) {
            return template;
        }

        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String reference = matcher.group(1); // e.g., "node-start::001.user_input"
            Object value = inputs.get(reference);
            if (value == null) {
                continue;
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(String.valueOf(value)));
        }

        matcher.appendTail(result);
        return result.toString();
    }
}
