package io.github.ashiqkodali.eazyreport.engine;

import io.github.ashiqkodali.eazyreport.expressions.Formatters;
import io.github.ashiqkodali.eazyreport.model.LayoutElement;

import java.util.List;
import java.util.Locale;

public class ChartGenerator {

    private static final String[] PALETTE = {
            "#3b82f6", "#10b981", "#f59e0b", "#ef4444", "#8b5cf6",
            "#06b6d4", "#ec4899", "#84cc16", "#6366f1", "#14b8a6"
    };

    public static String generateSvg(LayoutElement el, List<String> labels, List<Double> values) {
        double width = el.w > 0 ? el.w : 100.0;
        double height = el.h > 0 ? el.h : 60.0;
        String type = el.chartType != null ? el.chartType.toLowerCase() : "column";

        StringBuilder svg = new StringBuilder();
        svg.append(String.format(
                Locale.US,
                "<svg viewBox=\"0 0 %.1f %.1f\" style=\"width:100%%;height:100%%;display:block;\" xmlns=\"http://www.w3.org/2000/svg\">",
                width, height
        ));

        // Background
        String bg = el.box.background != null && !el.box.background.isEmpty() ? el.box.background : "transparent";
        if (!"transparent".equalsIgnoreCase(bg)) {
            svg.append(String.format(Locale.US, "<rect width=\"%.1f\" height=\"%.1f\" fill=\"%s\" />", width, height, bg));
        }

        // Title
        double titleH = 0.0;
        if (el.chartTitle != null && !el.chartTitle.isEmpty()) {
            titleH = 8.0;
            svg.append(String.format(
                    Locale.US,
                    "<text x=\"%.1f\" y=\"6\" text-anchor=\"middle\" font-family=\"sans-serif\" font-size=\"3.5\" font-weight=\"bold\" fill=\"#1e293b\">%s</text>",
                    width / 2.0, Formatters.escapeHtml(el.chartTitle)
            ));
        }

        if (values == null || values.isEmpty()) {
            svg.append("</svg>");
            return svg.toString();
        }

        double padX = 12.0;
        double padY = 8.0;
        double plotX = padX;
        double plotY = titleH + padY;
        double plotW = Math.max(10.0, width - (padX * 2));
        double plotH = Math.max(10.0, height - titleH - (padY * 2));

        if ("pie".equals(type) || "doughnut".equals(type)) {
            renderPieOrDoughnut(svg, labels, values, width, height, titleH, "doughnut".equals(type));
        } else if ("bar".equals(type)) {
            renderBarChart(svg, labels, values, plotX, plotY, plotW, plotH);
        } else if ("line".equals(type) || "area".equals(type)) {
            renderLineOrArea(svg, labels, values, plotX, plotY, plotW, plotH, "area".equals(type));
        } else {
            // Default: Column
            renderColumnChart(svg, labels, values, plotX, plotY, plotW, plotH);
        }

        svg.append("</svg>");
        return svg.toString();
    }

    private static void renderColumnChart(StringBuilder svg, List<String> labels, List<Double> values,
                                          double x, double y, double w, double h) {
        double maxVal = 1.0;
        for (Double v : values) if (v != null && v > maxVal) maxVal = v;

        int n = values.size();
        double barSlot = w / n;
        double barWidth = barSlot * 0.6;
        double barGap = (barSlot - barWidth) / 2.0;

        // Base axis line
        svg.append(String.format(Locale.US, "<line x1=\"%.1f\" y1=\"%.1f\" x2=\"%.1f\" y2=\"%.1f\" stroke=\"#cbd5e1\" stroke-width=\"0.5\" />",
                x, y + h, x + w, y + h));

        for (int i = 0; i < n; i++) {
            double v = values.get(i) != null ? values.get(i) : 0.0;
            double bh = (v / maxVal) * h;
            double bx = x + (i * barSlot) + barGap;
            double by = y + h - bh;
            String color = PALETTE[i % PALETTE.length];

            svg.append(String.format(
                    Locale.US,
                    "<rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" fill=\"%s\" rx=\"0.5\" />",
                    bx, by, barWidth, bh, color
            ));

            // Label
            if (labels != null && i < labels.size()) {
                String lbl = Formatters.escapeHtml(labels.get(i));
                svg.append(String.format(
                        Locale.US,
                        "<text x=\"%.1f\" y=\"%.1f\" text-anchor=\"middle\" font-family=\"sans-serif\" font-size=\"2.2\" fill=\"#64748b\">%s</text>",
                        bx + (barWidth / 2.0), y + h + 3.0, lbl
                ));
            }
        }
    }

    private static void renderBarChart(StringBuilder svg, List<String> labels, List<Double> values,
                                       double x, double y, double w, double h) {
        double maxVal = 1.0;
        for (Double v : values) if (v != null && v > maxVal) maxVal = v;

        int n = values.size();
        double slot = h / n;
        double barH = slot * 0.6;
        double gap = (slot - barH) / 2.0;

        for (int i = 0; i < n; i++) {
            double v = values.get(i) != null ? values.get(i) : 0.0;
            double bw = (v / maxVal) * w;
            double by = y + (i * slot) + gap;
            String color = PALETTE[i % PALETTE.length];

            svg.append(String.format(
                    Locale.US,
                    "<rect x=\"%.1f\" y=\"%.1f\" width=\"%.1f\" height=\"%.1f\" fill=\"%s\" rx=\"0.5\" />",
                    x, by, bw, barH, color
            ));

            if (labels != null && i < labels.size()) {
                String lbl = Formatters.escapeHtml(labels.get(i));
                svg.append(String.format(
                        Locale.US,
                        "<text x=\"%.1f\" y=\"%.1f\" text-anchor=\"end\" font-family=\"sans-serif\" font-size=\"2.2\" fill=\"#64748b\">%s</text>",
                        x - 1.5, by + (barH / 2.0) + 0.8, lbl
                ));
            }
        }
    }

    private static void renderLineOrArea(StringBuilder svg, List<String> labels, List<Double> values,
                                         double x, double y, double w, double h, boolean isArea) {
        double maxVal = 1.0;
        for (Double v : values) if (v != null && v > maxVal) maxVal = v;

        int n = values.size();
        if (n < 2) return;

        double step = w / (n - 1);
        StringBuilder pts = new StringBuilder();

        for (int i = 0; i < n; i++) {
            double v = values.get(i) != null ? values.get(i) : 0.0;
            double px = x + (i * step);
            double py = y + h - ((v / maxVal) * h);
            pts.append(String.format(Locale.US, "%.1f,%.1f ", px, py));
        }

        if (isArea) {
            String areaPts = String.format(Locale.US, "%.1f,%.1f %s %.1f,%.1f", x, y + h, pts.toString().trim(), x + w, y + h);
            svg.append(String.format(Locale.US, "<polygon points=\"%s\" fill=\"#3b82f6\" opacity=\"0.2\" />", areaPts));
        }

        svg.append(String.format(Locale.US, "<polyline points=\"%s\" fill=\"none\" stroke=\"#3b82f6\" stroke-width=\"1.2\" />", pts.toString().trim()));
    }

    private static void renderPieOrDoughnut(StringBuilder svg, List<String> labels, List<Double> values,
                                            double w, double h, double titleH, boolean isDoughnut) {
        double total = 0.0;
        for (Double v : values) if (v != null) total += v;
        if (total <= 0) return;

        double cx = w / 2.0;
        double cy = titleH + ((h - titleH) / 2.0);
        double r = Math.min(w, h - titleH) * 0.4;
        double startAngle = 0.0;

        for (int i = 0; i < values.size(); i++) {
            double v = values.get(i) != null ? values.get(i) : 0.0;
            double angle = (v / total) * 360.0;
            double endAngle = startAngle + angle;

            double x1 = cx + (r * Math.cos(Math.toRadians(startAngle)));
            double y1 = cy + (r * Math.sin(Math.toRadians(startAngle)));
            double x2 = cx + (r * Math.cos(Math.toRadians(endAngle)));
            double y2 = cy + (r * Math.sin(Math.toRadians(endAngle)));

            int largeArc = angle > 180 ? 1 : 0;
            String color = PALETTE[i % PALETTE.length];

            String d = String.format(Locale.US, "M %.1f,%.1f L %.1f,%.1f A %.1f,%.1f 0 %d 1 %.1f,%.1f Z",
                    cx, cy, x1, y1, r, r, largeArc, x2, y2);

            svg.append(String.format(Locale.US, "<path d=\"%s\" fill=\"%s\" />", d, color));
            startAngle = endAngle;
        }

        if (isDoughnut) {
            double innerR = r * 0.55;
            svg.append(String.format(Locale.US, "<circle cx=\"%.1f\" cy=\"%.1f\" r=\"%.1f\" fill=\"#ffffff\" />", cx, cy, innerR));
        }
    }
}
