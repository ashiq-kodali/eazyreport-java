package io.github.ashiqkodali.eazyreport.engine;

import io.github.ashiqkodali.eazyreport.expressions.ExpressionEngine;
import io.github.ashiqkodali.eazyreport.model.LayoutElement;
import io.github.ashiqkodali.eazyreport.model.Styles;

import java.util.HashMap;
import java.util.Map;

public class LogicEvaluator {

    public static void applyRules(LayoutElement element, RenderCtx rc) {
        if (element.rules == null || element.rules.isEmpty()) return;

        Map<String, Object> ctx = BandLayout.buildContextObject(rc);

        for (Styles.LogicRule rule : element.rules) {
            if (rule.condition == null || rule.condition.isEmpty()) continue;

            if (ExpressionEngine.evaluateCondition(rule.condition, ctx)) {
                if (rule.actions != null) {
                    applyActions(element, rule.actions, ctx);
                }
            }
        }
    }

    private static void applyActions(LayoutElement element, Map<String, Object> actions, Map<String, Object> ctx) {
        for (Map.Entry<String, Object> entry : actions.entrySet()) {
            String key = entry.getKey();
            Object val = entry.getValue();

            switch (key) {
                case "color":
                case "textColor":
                    element.style.color = String.valueOf(val);
                    break;
                case "background":
                case "backgroundColor":
                    element.box.background = String.valueOf(val);
                    break;
                case "visible":
                    boolean v = val instanceof Boolean ? (Boolean) val : Boolean.parseBoolean(String.valueOf(val));
                    element.visible = v;
                    if (!v) element.opacity = 0.0;
                    break;
                case "bold":
                    element.style.bold = val instanceof Boolean ? (Boolean) val : Boolean.parseBoolean(String.valueOf(val));
                    break;
                case "text":
                case "content":
                    element.content = ExpressionEngine.evaluateTemplate(String.valueOf(val), ctx);
                    break;
            }
        }
    }
}
