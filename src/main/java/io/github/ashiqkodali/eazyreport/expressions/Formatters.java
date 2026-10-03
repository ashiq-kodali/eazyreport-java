package io.github.ashiqkodali.eazyreport.expressions;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Formatters {

    public static String formatCurrency(Object value, String currencySymbol, int decimals) {
        if (value == null) return "";
        double val = toDouble(value);
        String curr = currencySymbol != null ? currencySymbol : "$";

        StringBuilder pattern = new StringBuilder("#,##0");
        if (decimals > 0) {
            pattern.append(".");
            for (int i = 0; i < decimals; i++) pattern.append("0");
        }

        DecimalFormat df = new DecimalFormat(pattern.toString(), new DecimalFormatSymbols(Locale.US));
        return curr + df.format(val);
    }

    public static String formatNumber(Object value, int decimals, boolean thousands) {
        if (value == null) return "";
        double val = toDouble(value);

        StringBuilder pattern = new StringBuilder(thousands ? "#,##0" : "0");
        if (decimals > 0) {
            pattern.append(".");
            for (int i = 0; i < decimals; i++) pattern.append("0");
        }

        DecimalFormat df = new DecimalFormat(pattern.toString(), new DecimalFormatSymbols(Locale.US));
        return df.format(val);
    }

    public static String formatDatePattern(Object value, String pattern) {
        if (value == null) return "";
        String str = value.toString();
        String pat = (pattern == null || pattern.isEmpty()) ? "yyyy-MM-dd" : pattern;

        // Map common PHP/Moment patterns to Java DateTimeFormatter
        pat = pat.replace("Y", "yyyy").replace("m", "MM").replace("d", "dd")
                .replace("H", "HH").replace("i", "mm").replace("s", "ss");

        try {
            if (str.length() == 10 && str.charAt(4) == '-' && str.charAt(7) == '-') {
                LocalDate ld = LocalDate.parse(str);
                return ld.format(DateTimeFormatter.ofPattern(pat, Locale.US));
            } else if (str.contains("T") || str.contains(" ")) {
                LocalDateTime ldt = LocalDateTime.parse(str.replace(" ", "T"));
                return ldt.format(DateTimeFormatter.ofPattern(pat, Locale.US));
            }
        } catch (Exception ignored) {}

        return str;
    }

    public static double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            String s = value.toString().replaceAll("[^0-9.-]", "");
            return Double.parseDouble(s);
        } catch (Exception e) {
            return 0.0;
        }
    }

    public static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#039;");
    }
}
