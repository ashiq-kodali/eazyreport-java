package io.github.ashiqkodali.eazyreport.renderers;

import io.github.ashiqkodali.eazyreport.engine.RenderedPage;
import io.github.ashiqkodali.eazyreport.expressions.Formatters;
import io.github.ashiqkodali.eazyreport.model.Styles;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HtmlRenderer {

    public static String render(List<RenderedPage> pages, Map<String, Object> options) {
        boolean standalone = options == null || !options.containsKey("standalone") || Boolean.parseBoolean(String.valueOf(options.get("standalone")));
        String title = (options != null && options.containsKey("title")) ? String.valueOf(options.get("title")) : "EazyReport";

        StringBuilder bodyContent = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) {
            bodyContent.append(renderPage(pages.get(i), i + 1, pages.size()));
        }

        if (!standalone) {
            return String.format("<div class=\"eazyreport-container\" style=\"display:flex;flex-direction:column;align-items:center;gap:20px;\">%s</div>", bodyContent);
        }

        RenderedPage firstPage = pages.isEmpty() ? null : pages.get(0);
        double pw = firstPage != null ? firstPage.width : 210.0;
        double ph = firstPage != null ? firstPage.height : 297.0;

        return String.format(
                Locale.US,
                "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>%s</title>\n" +
                "    <style>\n" +
                "        @page {\n" +
                "            size: %.2fmm %.2fmm;\n" +
                "            margin: 0;\n" +
                "        }\n" +
                "        *, *::before, *::after {\n" +
                "            box-sizing: border-box;\n" +
                "            -webkit-print-color-adjust: exact !important;\n" +
                "            print-color-adjust: exact !important;\n" +
                "        }\n" +
                "        body {\n" +
                "            margin: 0;\n" +
                "            padding: 0;\n" +
                "            background-color: #f1f5f9;\n" +
                "            font-family: -apple-system, BlinkMacSystemFont, \"Segoe UI\", Roboto, \"Helvetica Neue\", Arial, sans-serif;\n" +
                "            display: flex;\n" +
                "            flex-direction: column;\n" +
                "            align-items: center;\n" +
                "            gap: 24px;\n" +
                "            padding: 24px 0;\n" +
                "        }\n" +
                "        .eazyreport-page {\n" +
                "            position: relative;\n" +
                "            background-color: #ffffff;\n" +
                "            box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -2px rgba(0, 0, 0, 0.1);\n" +
                "            overflow: hidden;\n" +
                "            page-break-after: always;\n" +
                "            break-after: page;\n" +
                "        }\n" +
                "        .eazyreport-page:last-child {\n" +
                "            page-break-after: avoid;\n" +
                "            break-after: avoid;\n" +
                "        }\n" +
                "        .eazyreport-watermark {\n" +
                "            position: absolute;\n" +
                "            top: 50%%;\n" +
                "            left: 50%%;\n" +
                "            transform: translate(-50%%, -50%%);\n" +
                "            pointer-events: none;\n" +
                "            z-index: 0;\n" +
                "            user-select: none;\n" +
                "            white-space: nowrap;\n" +
                "        }\n" +
                "        .eazyreport-content {\n" +
                "            position: relative;\n" +
                "            width: 100%%;\n" +
                "            height: 100%%;\n" +
                "            z-index: 1;\n" +
                "        }\n" +
                "        @media print {\n" +
                "            body {\n" +
                "                background-color: #ffffff !important;\n" +
                "                padding: 0 !important;\n" +
                "                gap: 0 !important;\n" +
                "            }\n" +
                "            .eazyreport-page {\n" +
                "                box-shadow: none !important;\n" +
                "                margin: 0 !important;\n" +
                "            }\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "%s\n" +
                "</body>\n" +
                "</html>",
                title, pw, ph, bodyContent
        );
    }

    private static String renderPage(RenderedPage page, int pageNumber, int totalPages) {
        String wmHtml = renderWatermark(page.watermark);

        return String.format(
                Locale.US,
                "<div class=\"eazyreport-page\" data-page=\"%d\" style=\"width:%.2fmm;height:%.2fmm;min-width:%.2fmm;min-height:%.2fmm;\">" +
                "%s<div class=\"eazyreport-content\">%s</div></div>",
                pageNumber, page.width, page.height, page.width, page.height, wmHtml, page.html
        );
    }

    private static String renderWatermark(Styles.Watermark wm) {
        if (wm == null || !wm.enabled) return "";

        if ("image".equalsIgnoreCase(wm.type) && wm.imageSrc != null && !wm.imageSrc.isEmpty()) {
            return String.format(
                    Locale.US,
                    "<div class=\"eazyreport-watermark\" style=\"opacity:%.2f;transform:translate(-50%%, -50%%) rotate(%.1fdeg);\">" +
                    "<img src=\"%s\" style=\"max-width:80%%;max-height:80%%;object-fit:contain;\" /></div>",
                    wm.opacity, wm.rotation, Formatters.escapeHtml(wm.imageSrc)
            );
        }

        if (wm.text != null && !wm.text.isEmpty()) {
            return String.format(
                    Locale.US,
                    "<div class=\"eazyreport-watermark\" style=\"opacity:%.2f;transform:translate(-50%%, -50%%) rotate(%.1fdeg);" +
                    "font-size:%.1fpt;color:%s;font-weight:bold;font-family:sans-serif;letter-spacing:4px;\">%s</div>",
                    wm.opacity, wm.rotation, wm.fontSize, wm.color, Formatters.escapeHtml(wm.text)
            );
        }

        return "";
    }
}
