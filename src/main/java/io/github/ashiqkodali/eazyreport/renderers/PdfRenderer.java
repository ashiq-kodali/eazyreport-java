package io.github.ashiqkodali.eazyreport.renderers;

import io.github.ashiqkodali.eazyreport.engine.BandLayout;
import io.github.ashiqkodali.eazyreport.engine.RenderedPage;
import io.github.ashiqkodali.eazyreport.model.LayoutElement;
import io.github.ashiqkodali.eazyreport.model.Styles;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class PdfRenderer {

    private static final double MM_TO_PT = 2.83464567; // 72 / 25.4

    public static byte[] render(List<RenderedPage> pages, Map<String, Object> options) {
        List<String> objects = new ArrayList<>();
        // 1-indexed helper: index 0 is null
        objects.add(null);

        // Obj 1: Font Helvetica
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>");
        int fontObj = 1;

        // Obj 2: Font Helvetica-Bold
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>");
        int fontBoldObj = 2;

        // Obj 3: Font Helvetica-Oblique
        objects.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Oblique >>");
        int fontItalicObj = 3;

        List<Integer> pageStreamIds = new ArrayList<>();

        for (RenderedPage page : pages) {
            double pwPt = page.width * MM_TO_PT;
            double phPt = page.height * MM_TO_PT;

            StringBuilder stream = new StringBuilder("q\n");

            // Draw bands and placed elements
            for (BandLayout.PlacedBandRecord bandRecord : page.placedBands) {
                stream.append(renderBandNative(bandRecord, phPt));
            }

            // Draw watermark
            if (page.watermark != null && page.watermark.enabled && page.watermark.text != null && !page.watermark.text.isEmpty()) {
                stream.append(renderWatermarkNative(page.watermark, pwPt, phPt));
            }

            stream.append("Q\n");

            byte[] streamBytes = stream.toString().getBytes(StandardCharsets.ISO_8859_1);
            String streamObj = String.format(Locale.US, "<< /Length %d >>\nstream\n%s\nendstream", streamBytes.length, stream.toString());
            objects.add(streamObj);
            pageStreamIds.add(objects.size() - 1);
        }

        // Pages Tree Obj placeholder
        int pagesTreeObjId = objects.size();
        objects.add(""); // Placeholder

        List<Integer> pageObjIds = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) {
            RenderedPage page = pages.get(i);
            double pwPt = page.width * MM_TO_PT;
            double phPt = page.height * MM_TO_PT;
            int streamId = pageStreamIds.get(i);

            String pageObj = String.format(
                    Locale.US,
                    "<< /Type /Page /Parent %d 0 R /MediaBox [0 0 %.2f %.2f] /Contents %d 0 R /Resources << /Font << /F1 %d 0 R /F2 %d 0 R /F3 %d 0 R >> >> >>",
                    pagesTreeObjId, pwPt, phPt, streamId, fontObj, fontBoldObj, fontItalicObj
            );
            objects.add(pageObj);
            pageObjIds.add(objects.size() - 1);
        }

        // Fill in Pages Tree Obj
        StringBuilder kids = new StringBuilder();
        for (int id : pageObjIds) kids.append(id).append(" 0 R ");
        objects.set(pagesTreeObjId, String.format(Locale.US, "<< /Type /Pages /Kids [%s] /Count %d >>", kids.toString().trim(), pageObjIds.size()));

        // Catalog Obj
        int catalogObjId = objects.size();
        objects.add(String.format(Locale.US, "<< /Type /Catalog /Pages %d 0 R >>", pagesTreeObjId));

        // Assemble PDF byte stream
        StringBuilder out = new StringBuilder("%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n");
        List<Integer> xrefOffsets = new ArrayList<>();
        xrefOffsets.add(0);

        int totalObjCount = objects.size() - 1;
        for (int i = 1; i <= totalObjCount; i++) {
            xrefOffsets.add(out.length());
            out.append(String.format(Locale.US, "%d 0 obj\n%s\nendobj\n", i, objects.get(i)));
        }

        int xrefStart = out.length();
        out.append(String.format(Locale.US, "xref\n0 %d\n0000000000 65535 f \n", totalObjCount + 1));
        for (int i = 1; i <= totalObjCount; i++) {
            out.append(String.format(Locale.US, "%010d 00000 n \n", xrefOffsets.get(i)));
        }

        out.append(String.format(Locale.US, "trailer\n<< /Size %d /Root %d 0 R >>\nstartxref\n%d\n%%%%EOF\n",
                totalObjCount + 1, catalogObjId, xrefStart));

        return out.toString().getBytes(StandardCharsets.ISO_8859_1);
    }

    private static String renderBandNative(BandLayout.PlacedBandRecord band, double pageHeightPt) {
        StringBuilder out = new StringBuilder();
        double bxPt = band.x * MM_TO_PT;
        double byPt = band.y * MM_TO_PT;
        double bwPt = band.width * MM_TO_PT;
        double bhPt = band.height * MM_TO_PT;

        // Band background
        if (band.fill != null && !band.fill.isEmpty() && !"transparent".equalsIgnoreCase(band.fill)) {
            double[] rgb = parseHexColor(band.fill);
            if (rgb != null) {
                double pdfY = pageHeightPt - byPt - bhPt;
                out.append(String.format(Locale.US, "%.3f %.3f %.3f rg\n%.2f %.2f %.2f %.2f re f\n",
                        rgb[0], rgb[1], rgb[2], bxPt, pdfY, bwPt, bhPt));
            }
        }

        for (BandLayout.PlacedElementRecord placedEl : band.elements) {
            out.append(renderElementNative(placedEl, bxPt, byPt, pageHeightPt));
        }

        return out.toString();
    }

    private static String renderElementNative(BandLayout.PlacedElementRecord placed, double bandLeftPt, double bandTopPt, double pageHeightPt) {
        LayoutElement el = placed.element;
        double xPt = bandLeftPt + (placed.x * MM_TO_PT);
        double yPt = bandTopPt + (placed.y * MM_TO_PT);
        double wPt = placed.w * MM_TO_PT;
        double hPt = placed.h * MM_TO_PT;
        double pdfY = pageHeightPt - yPt - hPt;

        StringBuilder out = new StringBuilder();

        // Box background
        if (el.box.background != null && !el.box.background.isEmpty() && !"transparent".equalsIgnoreCase(el.box.background)) {
            double[] bgRgb = parseHexColor(el.box.background);
            if (bgRgb != null) {
                out.append(String.format(Locale.US, "%.3f %.3f %.3f rg\n%.2f %.2f %.2f %.2f re f\n",
                        bgRgb[0], bgRgb[1], bgRgb[2], xPt, pdfY, wPt, hPt));
            }
        }

        // Box border
        if (el.box.borderWidth > 0 && el.box.borderColor != null && !el.box.borderColor.isEmpty()) {
            double[] borderRgb = parseHexColor(el.box.borderColor);
            if (borderRgb != null) {
                double bw = Math.max(0.5, el.box.borderWidth * MM_TO_PT);
                out.append(String.format(Locale.US, "%.3f %.3f %.3f RG\n%.2f w\n%.2f %.2f %.2f %.2f re S\n",
                        borderRgb[0], borderRgb[1], borderRgb[2], bw, xPt, pdfY, wPt, hPt));
            }
        }

        // Shape / Line
        String type = el.type != null ? el.type.toLowerCase() : "text";
        if ("shape".equals(type) || "line".equals(type)) {
            String shape = el.shapeType != null ? el.shapeType.toLowerCase() : "rectangle";
            if ("line".equals(shape)) {
                double[] strokeRgb = parseHexColor(el.stroke != null && !el.stroke.isEmpty() ? el.stroke : (el.box.borderColor != null ? el.box.borderColor : "#000000"));
                if (strokeRgb == null) strokeRgb = new double[]{0, 0, 0};
                double sw = Math.max(0.5, (el.strokeWidth > 0 ? el.strokeWidth : el.box.borderWidth) * MM_TO_PT);
                double lineY = pdfY + (hPt / 2.0);
                out.append(String.format(Locale.US, "%.3f %.3f %.3f RG\n%.2f w\n%.2f %.2f m %.2f %.2f l S\n",
                        strokeRgb[0], strokeRgb[1], strokeRgb[2], sw, xPt, lineY, xPt + wPt, lineY));
            }
            return out.toString();
        }

        // Text
        if (placed.text != null && !placed.text.isEmpty()) {
            String fontTag = "/F1";
            if (el.style.bold) {
                fontTag = "/F2";
            } else if (el.style.italic) {
                fontTag = "/F3";
            }

            double fontSize = Math.max(4.0, el.style.fontSize);
            double[] colorRgb = parseHexColor(el.style.color);
            if (colorRgb == null) colorRgb = new double[]{0, 0, 0};

            String[] lines = placed.text.split("\n");
            double lineH = fontSize * el.style.lineHeight;

            double padTop = el.box.getPaddingTop() * MM_TO_PT;
            double padLeft = el.box.getPaddingLeft() * MM_TO_PT;
            double textY = pdfY + hPt - padTop - fontSize;

            for (int lineIdx = 0; lineIdx < lines.length; lineIdx++) {
                double curY = textY - (lineIdx * lineH);
                if (curY < pdfY) break;

                double curX = xPt + padLeft;
                String escaped = lines[lineIdx].replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");

                out.append(String.format(
                        Locale.US,
                        "BT\n%s %.1f Tf\n%.3f %.3f %.3f rg\n1 0 0 1 %.2f %.2f Tm\n(%s) Tj\nET\n",
                        fontTag, fontSize, colorRgb[0], colorRgb[1], colorRgb[2], curX, curY, escaped
                ));
            }
        }

        return out.toString();
    }

    private static String renderWatermarkNative(Styles.Watermark wm, double pwPt, double phPt) {
        double fontSize = Math.max(12.0, wm.fontSize);
        double[] rgb = parseHexColor(wm.color);
        if (rgb == null) rgb = new double[]{0.8, 0.8, 0.8};

        String escaped = wm.text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
        double rad = Math.toRadians(wm.rotation);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        double cx = pwPt / 2.0;
        double cy = phPt / 2.0;

        return String.format(
                Locale.US,
                "q\nBT\n/F2 %.1f Tf\n%.3f %.3f %.3f rg\n%.4f %.4f %.4f %.4f %.2f %.2f Tm\n(%s) Tj\nET\nQ\n",
                fontSize, rgb[0], rgb[1], rgb[2], cos, sin, -sin, cos, cx - 100, cy, escaped
        );
    }

    private static double[] parseHexColor(String hex) {
        if (hex == null) return null;
        String h = hex.trim();
        if (h.isEmpty() || "transparent".equalsIgnoreCase(h)) return null;
        if (h.startsWith("#")) h = h.substring(1);

        try {
            if (h.length() == 3) {
                double r = Integer.parseInt(h.substring(0, 1) + h.substring(0, 1), 16) / 255.0;
                double g = Integer.parseInt(h.substring(1, 2) + h.substring(1, 2), 16) / 255.0;
                double b = Integer.parseInt(h.substring(2, 3) + h.substring(2, 3), 16) / 255.0;
                return new double[]{r, g, b};
            }
            if (h.length() >= 6) {
                double r = Integer.parseInt(h.substring(0, 2), 16) / 255.0;
                double g = Integer.parseInt(h.substring(2, 4), 16) / 255.0;
                double b = Integer.parseInt(h.substring(4, 6), 16) / 255.0;
                return new double[]{r, g, b};
            }
        } catch (Exception ignored) {}

        return null;
    }
}
