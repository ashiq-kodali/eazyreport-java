package io.github.ashiqkodali.eazyreport.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.ashiqkodali.eazyreport.expressions.ExpressionEngine;
import io.github.ashiqkodali.eazyreport.expressions.Formatters;
import io.github.ashiqkodali.eazyreport.model.Band;
import io.github.ashiqkodali.eazyreport.model.LayoutElement;
import io.github.ashiqkodali.eazyreport.model.Styles;

import java.util.*;

public class BandLayout {

    private static final ObjectMapper mapper = new ObjectMapper();

    public static class PlacedElementRecord {
        public LayoutElement element;
        public double x;
        public double y;
        public double w;
        public double h;
        public String text;
        public RenderCtx context;

        public PlacedElementRecord(LayoutElement el, double x, double y, double w, double h, String text, RenderCtx ctx) {
            this.element = el;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.text = text;
            this.context = ctx;
        }
    }

    public static class PlacedBandRecord {
        public Band band;
        public double x;
        public double y;
        public double width;
        public double height;
        public String fill;
        public List<PlacedElementRecord> elements;

        public PlacedBandRecord(Band band, double x, double y, double width, double height, String fill, List<PlacedElementRecord> elements) {
            this.band = band;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.fill = fill;
            this.elements = elements;
        }
    }

    public static class LaidOutBand {
        public String html;
        public double height;
        public List<PlacedElementRecord> elements;

        public LaidOutBand(String html, double height, List<PlacedElementRecord> elements) {
            this.html = html;
            this.height = height;
            this.elements = elements;
        }
    }

    public static String fieldText(LayoutElement el, RenderCtx rc) {
        String raw = el.resolveRawBinding();

        if (raw == null || raw.isEmpty()) return "";

        if (raw.contains("{{")) {
            Map<String, Object> ctx = buildContextObject(rc);
            return ExpressionEngine.evaluateTemplate(raw, ctx);
        }

        Object val = rc.resolveValue(raw);
        if (val == null && raw.contains(" ")) {
            val = ExpressionEngine.evaluateExpression(raw, buildContextObject(rc));
        }

        if (val == null) return raw;

        Styles.ValueFormat fmt = el.format;
        if (fmt != null && fmt.type != null && !fmt.type.isEmpty() && !"text".equalsIgnoreCase(fmt.type)) {
            String fmtType = fmt.type.toLowerCase();
            switch (fmtType) {
                case "currency":
                    return Formatters.formatCurrency(val, fmt.currency, fmt.decimals);
                case "number":
                    return Formatters.formatNumber(val, fmt.decimals, fmt.thousands);
                case "date":
                    return Formatters.formatDatePattern(val, fmt.pattern);
                case "percent":
                    double d = Formatters.toDouble(val);
                    return String.format(Locale.US, "%." + fmt.decimals + "f%%", d);
            }
        }

        return val.toString();
    }

    public static double measureElementHeight(LayoutElement el, String text) {
        if (!el.autoHeight || text == null || text.isEmpty()) {
            return el.h;
        }

        double pt = 0.3528;
        double lineH = el.style.fontSize * el.style.lineHeight * pt;
        double charW = el.style.fontSize * 0.52 * pt;
        double padding = el.box.getPaddingTop() + el.box.getPaddingBottom();

        double availW = Math.max(1.0, el.w - el.box.getPaddingLeft() - el.box.getPaddingRight());
        int charsPerLine = Math.max(1, (int) (availW / charW));

        int lines = 0;
        for (String line : text.split("\n")) {
            lines += Math.max(1, (int) Math.ceil((double) line.length() / charsPerLine));
        }

        double neededH = lines * lineH + padding;
        return Math.max(el.h, neededH);
    }

    public static LaidOutBand layout(Band band, RenderCtx rc, double width, String fill, Double fixedHeight) {
        if (band.visibleExpr != null && !band.visibleExpr.isEmpty()) {
            Map<String, Object> ctx = buildContextObject(rc);
            if (!ExpressionEngine.evaluateCondition(band.visibleExpr, ctx)) {
                return null;
            }
        }

        List<PlacedElementRecord> placedElements = new ArrayList<>();
        double maxBottom = fixedHeight != null ? fixedHeight : band.height;
        Map<String, Double> elemHeights = new HashMap<>();

        for (LayoutElement el : band.elements) {
            String txt = fieldText(el, rc);
            double eh = measureElementHeight(el, txt);
            elemHeights.put(el.id, eh);

            if (band.canGrow && (el.y + eh > maxBottom)) {
                maxBottom = el.y + eh;
            }
        }

        String bandFill = (fill != null && !fill.isEmpty()) ? fill : band.fill;
        String bgStyle = (bandFill != null && !bandFill.isEmpty() && !"transparent".equalsIgnoreCase(bandFill))
                ? String.format("background-color:%s;", bandFill) : "";

        StringBuilder html = new StringBuilder();
        html.append(String.format(
                Locale.US,
                "<div class=\"report-band\" style=\"position:relative;width:%.2fmm;height:%.2fmm;%sbox-sizing:border-box;\">",
                width, maxBottom, bgStyle
        ));

        for (LayoutElement el : band.elements) {
            LayoutElement elementClone = el.copy();
            LogicEvaluator.applyRules(elementClone, rc);

            if (elementClone.opacity <= 0.0 || !elementClone.visible) continue;

            String txt = fieldText(elementClone, rc);
            double eh = elemHeights.getOrDefault(el.id, elementClone.h);

            placedElements.add(new PlacedElementRecord(
                    elementClone,
                    elementClone.x,
                    elementClone.y,
                    elementClone.w,
                    eh,
                    txt,
                    rc.copy()
            ));

            String elHtml = renderElementHtml(elementClone, txt, eh, rc);
            html.append(String.format(
                    Locale.US,
                    "<div style=\"position:absolute;left:%.2fmm;top:%.2fmm;width:%.2fmm;height:%.2fmm;box-sizing:border-box;\">%s</div>",
                    elementClone.x, elementClone.y, elementClone.w, eh, elHtml
            ));
        }

        html.append("</div>");

        return new LaidOutBand(html.toString(), maxBottom, placedElements);
    }

    public static String renderElementHtml(LayoutElement el, String text, double actualH, RenderCtx rc) {
        String type = el.type != null ? el.type.toLowerCase() : "text";

        switch (type) {
            case "barcode":
                String bcVal = (el.barcodeValue != null && !el.barcodeValue.isEmpty()) ? el.barcodeValue
                        : (el.value != null && !el.value.isEmpty() ? el.value : text);
                if (bcVal != null && bcVal.contains("{{")) {
                    bcVal = ExpressionEngine.evaluateTemplate(bcVal, buildContextObject(rc));
                }
                return BarcodeGenerator.generateSvg(el.symbology,
                        bcVal,
                        el.barColor, el.barcodeBackground, el.showText);
            case "chart":
                return renderChartHtml(el, rc);
            case "shape":
            case "line":
                return renderShapeHtml(el);
            case "image":
                return renderImageHtml(el);
            case "checkbox":
                return renderCheckboxHtml(el, rc);
            default:
                return renderTextHtml(el, text, actualH);
        }
    }

    private static String renderTextHtml(LayoutElement el, String text, double actualH) {
        Styles.TextStyle s = el.style;
        Styles.BoxStyle b = el.box;

        StringBuilder css = new StringBuilder();
        css.append(String.format("font-family:'%s', sans-serif;", Formatters.escapeHtml(s.fontFamily)));
        css.append(String.format(Locale.US, "font-size:%.1fpt;", s.fontSize));
        css.append(String.format("color:%s;", s.color));
        css.append(String.format("text-align:%s;", s.align));
        css.append(String.format(Locale.US, "line-height:%.1f;", s.lineHeight));
        if (s.bold) css.append("font-weight:700;");
        if (s.italic) css.append("font-style:italic;");
        if (s.underline) css.append("text-decoration:underline;");

        if (b.background != null && !b.background.isEmpty()) {
            css.append(String.format("background-color:%s;", b.background));
        }
        if (b.borderWidth > 0 && b.borderColor != null && !b.borderColor.isEmpty()) {
            css.append(String.format(Locale.US, "border:%.1fmm %s %s;", b.borderWidth, b.borderStyle, b.borderColor));
        }
        if (b.borderRadius > 0) {
            css.append(String.format(Locale.US, "border-radius:%.1fmm;", b.borderRadius));
        }

        css.append(String.format(Locale.US, "padding:%.1fmm %.1fmm %.1fmm %.1fmm;",
                b.getPaddingTop(), b.getPaddingRight(), b.getPaddingBottom(), b.getPaddingLeft()));
        css.append("box-sizing:border-box;display:flex;width:100%;height:100%;overflow:hidden;word-break:break-word;");

        String justify = "flex-start";
        if ("center".equalsIgnoreCase(s.align)) justify = "center";
        else if ("right".equalsIgnoreCase(s.align)) justify = "flex-end";

        String align = "flex-start";
        if ("middle".equalsIgnoreCase(s.verticalAlign)) align = "center";
        else if ("bottom".equalsIgnoreCase(s.verticalAlign)) align = "flex-end";

        css.append(String.format("justify-content:%s;align-items:%s;", justify, align));

        String escText = Formatters.escapeHtml(text).replace("\n", "<br/>");
        return String.format("<div style=\"%s\">%s</div>", css, escText);
    }

    private static String renderShapeHtml(LayoutElement el) {
        String fill = (el.fill != null && !el.fill.isEmpty()) ? el.fill
                : (el.box.background != null && !el.box.background.isEmpty() ? el.box.background : "transparent");
        String stroke = (el.stroke != null && !el.stroke.isEmpty()) ? el.stroke
                : (el.box.borderColor != null && !el.box.borderColor.isEmpty() ? el.box.borderColor : "#000000");
        double strokeW = el.strokeWidth > 0 ? el.strokeWidth : (el.box.borderWidth > 0 ? el.box.borderWidth : 1.0);

        String shape = el.shapeType != null ? el.shapeType.toLowerCase() : "rectangle";
        StringBuilder svg = new StringBuilder();
        svg.append(String.format(Locale.US, "<svg viewBox=\"0 0 %.2f %.2f\" style=\"width:100%%;height:100%%;display:block;\" xmlns=\"http://www.w3.org/2000/svg\">", el.w, el.h));

        if ("circle".equals(shape) || "ellipse".equals(shape)) {
            svg.append(String.format(Locale.US, "<ellipse cx=\"%.2f\" cy=\"%.2f\" rx=\"%.2f\" ry=\"%.2f\" fill=\"%s\" stroke=\"%s\" stroke-width=\"%.2f\"/>",
                    el.w / 2.0, el.h / 2.0, (el.w - strokeW) / 2.0, (el.h - strokeW) / 2.0, fill, stroke, strokeW));
        } else if ("line".equals(shape)) {
            svg.append(String.format(Locale.US, "<line x1=\"0\" y1=\"%.2f\" x2=\"%.2f\" y2=\"%.2f\" stroke=\"%s\" stroke-width=\"%.2f\"/>",
                    el.h / 2.0, el.w, el.h / 2.0, stroke, strokeW));
        } else {
            // Rectangle / Rounded rect
            double rx = ("rounded_rect".equals(shape) || el.box.borderRadius > 0)
                    ? (el.box.borderRadius > 0 ? el.box.borderRadius : 3.0) : 0.0;
            svg.append(String.format(Locale.US, "<rect x=\"%.2f\" y=\"%.2f\" width=\"%.2f\" height=\"%.2f\" rx=\"%.2f\" fill=\"%s\" stroke=\"%s\" stroke-width=\"%.2f\"/>",
                    strokeW / 2.0, strokeW / 2.0, el.w - strokeW, el.h - strokeW, rx, fill, stroke, strokeW));
        }

        svg.append("</svg>");
        return svg.toString();
    }

    private static String renderImageHtml(LayoutElement el) {
        if (el.src == null || el.src.isEmpty()) return "";
        String fit = "contain";
        if ("cover".equalsIgnoreCase(el.fit)) fit = "cover";
        else if ("fill".equalsIgnoreCase(el.fit)) fit = "fill";

        return String.format("<img src=\"%s\" style=\"width:100%%;height:100%%;object-fit:%s;display:block;\" />",
                Formatters.escapeHtml(el.src), fit);
    }

    private static String renderCheckboxHtml(LayoutElement el, RenderCtx rc) {
        boolean isChecked = false;
        if (el.checkedExpr != null && !el.checkedExpr.isEmpty()) {
            isChecked = ExpressionEngine.evaluateCondition(el.checkedExpr, buildContextObject(rc));
        }

        String checkChar = isChecked ? "&#10003;" : "";
        String border = el.box.borderWidth > 0
                ? String.format(Locale.US, "%.1fmm solid %s", el.box.borderWidth, el.box.borderColor)
                : "1.5px solid #000";
        String bg = (el.box.background != null && !el.box.background.isEmpty()) ? el.box.background : "#ffffff";

        return String.format("<div style=\"width:100%%;height:100%%;display:flex;align-items:center;justify-content:center;border:%s;background-color:%s;font-weight:bold;font-size:10pt;\">%s</div>",
                border, bg, checkChar);
    }

    private static String renderChartHtml(LayoutElement el, RenderCtx rc) {
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        Object list = (el.dataset != null && !el.dataset.isEmpty()) ? rc.resolveValue(el.dataset) : rc.data;
        if (list instanceof Iterable) {
            for (Object item : (Iterable<?>) list) {
                Object lVal = (el.labelField != null && !el.labelField.isEmpty()) ? ExpressionEngine.getPath(item, el.labelField) : null;
                Object vVal = (el.valueField != null && !el.valueField.isEmpty()) ? ExpressionEngine.getPath(item, el.valueField) : null;

                labels.add(lVal != null ? lVal.toString() : "");
                values.add(Formatters.toDouble(vVal));
            }
        }

        return ChartGenerator.generateSvg(el, labels, values);
    }

    public static Map<String, Object> buildContextObject(RenderCtx rc) {
        Map<String, Object> dict = new HashMap<>();
        dict.put("Page", rc.pageNumber);
        dict.put("TotalPages", rc.totalPages);
        dict.put("pageNumber", rc.pageNumber);
        dict.put("totalPages", rc.totalPages);

        if (rc.data != null) {
            dict.put("data", rc.data);
            dict.put("root", rc.data);
            if (rc.data instanceof Map) {
                dict.putAll((Map<String, Object>) rc.data);
            }
        }

        if (rc.item != null) {
            dict.put("item", rc.item);
            dict.put("this", rc.item);
            if (rc.item instanceof Map) {
                dict.putAll((Map<String, Object>) rc.item);
            }
        }

        dict.putAll(rc.params);
        return dict;
    }
}
