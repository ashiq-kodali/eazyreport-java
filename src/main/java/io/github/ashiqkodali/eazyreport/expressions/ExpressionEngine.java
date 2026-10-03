package io.github.ashiqkodali.eazyreport.expressions;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExpressionEngine {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static Object getPath(Object context, String path) {
        if (context == null || path == null || path.isEmpty()) {
            return context;
        }

        String[] parts = path.split("\\.");
        Object current = context;

        for (String part : parts) {
            if (current == null) return null;

            if (current instanceof Map) {
                current = ((Map<?, ?>) current).get(part);
            } else if (current instanceof List) {
                try {
                    int idx = Integer.parseInt(part);
                    List<?> list = (List<?>) current;
                    current = (idx >= 0 && idx < list.size()) ? list.get(idx) : null;
                } catch (NumberFormatException e) {
                    return null;
                }
            } else {
                // Try reflection getter or Jackson map conversion
                try {
                    Map<?, ?> map = mapper.convertValue(current, Map.class);
                    current = map.get(part);
                } catch (Exception e) {
                    return null;
                }
            }
        }

        return current;
    }

    public static String evaluateTemplate(String template, Object context) {
        if (template == null || !template.contains("{{")) {
            return template != null ? template : "";
        }

        String result = template;

        // 1. Process #if blocks: {{#if expr}}...{{else}}...{{/if}} or {{#if expr}}...{{/if}}
        Pattern ifPattern = Pattern.compile("\\{\\{#if\\s+([^}]+)\\}\\}([\\s\\S]*?)(?:\\{\\{else\\}\\}([\\s\\S]*?))?\\{\\{/if\\}\\}");
        Matcher ifMatcher = ifPattern.matcher(result);
        while (ifMatcher.find()) {
            String expr = ifMatcher.group(1).trim();
            String ifBody = ifMatcher.group(2);
            String elseBody = ifMatcher.group(3) != null ? ifMatcher.group(3) : "";

            boolean cond = evaluateCondition(expr, context);
            String replacement = cond ? evaluateTemplate(ifBody, context) : evaluateTemplate(elseBody, context);
            result = result.substring(0, ifMatcher.start()) + replacement + result.substring(ifMatcher.end());
            ifMatcher = ifPattern.matcher(result);
        }

        // 2. Process #each blocks: {{#each list}}...{{/each}}
        Pattern eachPattern = Pattern.compile("\\{\\{#each\\s+([^}]+)\\}\\}([\\s\\S]*?)\\{\\{/each\\}\\}");
        Matcher eachMatcher = eachPattern.matcher(result);
        while (eachMatcher.find()) {
            String listPath = eachMatcher.group(1).trim();
            String itemTemplate = eachMatcher.group(2);

            Object rawList = getPath(context, listPath);
            StringBuilder sb = new StringBuilder();

            if (rawList instanceof Iterable) {
                int index = 0;
                for (Object item : (Iterable<?>) rawList) {
                    Map<String, Object> subCtx = new HashMap<>();
                    if (context instanceof Map) {
                        subCtx.putAll((Map<String, Object>) context);
                    }
                    subCtx.put("this", item);
                    subCtx.put("@index", index++);
                    if (item instanceof Map) {
                        subCtx.putAll((Map<String, Object>) item);
                    }
                    sb.append(evaluateTemplate(itemTemplate, subCtx));
                }
            }

            result = result.substring(0, eachMatcher.start()) + sb.toString() + result.substring(eachMatcher.end());
            eachMatcher = eachPattern.matcher(result);
        }

        // 3. Process tags: {{path}} or {{helper arg1 arg2}}
        Pattern tagPattern = Pattern.compile("\\{\\{([^}]+)\\}\\}");
        Matcher tagMatcher = tagPattern.matcher(result);
        StringBuilder sb = new StringBuilder();

        while (tagMatcher.find()) {
            String tag = tagMatcher.group(1).trim();
            String val = evaluateTag(tag, context);
            tagMatcher.appendReplacement(sb, Matcher.quoteReplacement(val));
        }
        tagMatcher.appendTail(sb);

        return sb.toString();
    }

    private static String evaluateTag(String tag, Object context) {
        String[] parts = tag.split("\\s+");
        String first = parts[0];

        // Check helpers
        if (parts.length > 1) {
            String helper = first.toLowerCase();
            String arg1Str = parts[1];
            Object arg1Val = resolveArgument(arg1Str, context);

            switch (helper) {
                case "currency": {
                    String symbol = parts.length > 2 ? cleanArg(parts[2]) : "$";
                    int decimals = parts.length > 3 ? (int) Formatters.toDouble(parts[3]) : 2;
                    return Formatters.formatCurrency(arg1Val, symbol, decimals);
                }
                case "number": {
                    int decimals = parts.length > 2 ? (int) Formatters.toDouble(parts[2]) : 2;
                    boolean thousands = parts.length <= 3 || Boolean.parseBoolean(parts[3]);
                    return Formatters.formatNumber(arg1Val, decimals, thousands);
                }
                case "words":
                    return NumberToWords.convert(Formatters.toDouble(arg1Val));
                case "date": {
                    String pattern = parts.length > 2 ? cleanArg(parts[2]) : "yyyy-MM-dd";
                    return Formatters.formatDatePattern(arg1Val, pattern);
                }
                case "upper":
                case "uppercase":
                    return arg1Val != null ? arg1Val.toString().toUpperCase() : "";
                case "lower":
                case "lowercase":
                    return arg1Val != null ? arg1Val.toString().toLowerCase() : "";
                case "capitalize": {
                    String s = arg1Val != null ? arg1Val.toString() : "";
                    return s.isEmpty() ? "" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
                }
                case "add": {
                    double v1 = Formatters.toDouble(arg1Val);
                    double v2 = parts.length > 2 ? Formatters.toDouble(resolveArgument(parts[2], context)) : 0;
                    return String.valueOf(v1 + v2);
                }
                case "subtract": {
                    double v1 = Formatters.toDouble(arg1Val);
                    double v2 = parts.length > 2 ? Formatters.toDouble(resolveArgument(parts[2], context)) : 0;
                    return String.valueOf(v1 - v2);
                }
                case "multiply": {
                    double v1 = Formatters.toDouble(arg1Val);
                    double v2 = parts.length > 2 ? Formatters.toDouble(resolveArgument(parts[2], context)) : 0;
                    return String.valueOf(v1 * v2);
                }
                case "divide": {
                    double v1 = Formatters.toDouble(arg1Val);
                    double v2 = parts.length > 2 ? Formatters.toDouble(resolveArgument(parts[2], context)) : 1;
                    return String.valueOf(v2 != 0 ? v1 / v2 : 0);
                }
            }
        }

        // Direct path lookup
        Object val = getPath(context, tag);
        return val != null ? val.toString() : "";
    }

    private static Object resolveArgument(String arg, Object context) {
        arg = arg.trim();
        if ((arg.startsWith("'") && arg.endsWith("'")) || (arg.startsWith("\"") && arg.endsWith("\""))) {
            return arg.substring(1, arg.length() - 1);
        }
        try {
            return Double.parseDouble(arg);
        } catch (NumberFormatException e) {
            return getPath(context, arg);
        }
    }

    private static String cleanArg(String arg) {
        arg = arg.trim();
        if ((arg.startsWith("'") && arg.endsWith("'")) || (arg.startsWith("\"") && arg.endsWith("\""))) {
            return arg.substring(1, arg.length() - 1);
        }
        return arg;
    }

    public static boolean evaluateCondition(String expr, Object context) {
        expr = expr.trim();
        if (expr.isEmpty()) return true;

        String[] operators = {"==", "!=", ">=", "<=", ">", "<"};
        for (String op : operators) {
            if (expr.contains(op)) {
                String[] parts = expr.split(Pattern.quote(op), 2);
                Object left = resolveArgument(parts[0], context);
                Object right = resolveArgument(parts[1], context);

                double ld = Formatters.toDouble(left);
                double rd = Formatters.toDouble(right);

                boolean numeric = (left instanceof Number || !Double.isNaN(ld)) && (right instanceof Number || !Double.isNaN(rd));

                switch (op) {
                    case "==": return Objects.equals(String.valueOf(left), String.valueOf(right));
                    case "!=": return !Objects.equals(String.valueOf(left), String.valueOf(right));
                    case ">": return numeric && ld > rd;
                    case ">=": return numeric && ld >= rd;
                    case "<": return numeric && ld < rd;
                    case "<=": return numeric && ld <= rd;
                }
            }
        }

        Object val = getPath(context, expr);
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof Number) return ((Number) val).doubleValue() != 0;
        return val != null && !val.toString().isEmpty();
    }

    public static Object evaluateExpression(String expr, Object context) {
        expr = expr.trim();
        // Replace identifiers with numeric values
        Pattern p = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_.]*");
        Matcher m = p.matcher(expr);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String ident = m.group();
            Object val = getPath(context, ident);
            double num = Formatters.toDouble(val);
            m.appendReplacement(sb, String.valueOf(num));
        }
        m.appendTail(sb);

        // Simple arithmetic parser
        return evaluateSimpleMath(sb.toString());
    }

    private static double evaluateSimpleMath(String math) {
        try {
            // Remove spaces
            math = math.replaceAll("\\s+", "");
            // Handle parentheses recursively
            while (math.contains("(")) {
                int open = math.lastIndexOf('(');
                int close = math.indexOf(')', open);
                if (close == -1) break;
                double sub = evaluateSimpleMath(math.substring(open + 1, close));
                math = math.substring(0, open) + sub + math.substring(close + 1);
            }

            // Simple tokens: +, -, *, /
            List<Double> numbers = new ArrayList<>();
            List<Character> ops = new ArrayList<>();

            StringBuilder numBuf = new StringBuilder();
            for (int i = 0; i < math.length(); i++) {
                char c = math.charAt(i);
                if ((c == '-' && (i == 0 || "+-*/".indexOf(math.charAt(i - 1)) != -1)) || Character.isDigit(c) || c == '.') {
                    numBuf.append(c);
                } else if ("+-*/".indexOf(c) != -1) {
                    if (numBuf.length() > 0) {
                        numbers.add(Double.parseDouble(numBuf.toString()));
                        numBuf.setLength(0);
                    }
                    ops.add(c);
                }
            }
            if (numBuf.length() > 0) numbers.add(Double.parseDouble(numBuf.toString()));

            // * and /
            for (int i = 0; i < ops.size(); ) {
                char op = ops.get(i);
                if (op == '*' || op == '/') {
                    double n1 = numbers.get(i);
                    double n2 = numbers.get(i + 1);
                    double res = op == '*' ? n1 * n2 : (n2 != 0 ? n1 / n2 : 0);
                    numbers.set(i, res);
                    numbers.remove(i + 1);
                    ops.remove(i);
                } else {
                    i++;
                }
            }

            // + and -
            double total = numbers.isEmpty() ? 0 : numbers.get(0);
            for (int i = 0; i < ops.size(); i++) {
                char op = ops.get(i);
                double next = numbers.get(i + 1);
                if (op == '+') total += next;
                else if (op == '-') total -= next;
            }

            return total;
        } catch (Exception e) {
            return 0.0;
        }
    }
}
