package io.github.ashiqkodali.eazyreport.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BarcodeGenerator {

    // Code 128B patterns
    private static final String[] CODE128_PATTERNS = {
            "212222", "222122", "222221", "121223", "121322", "131222", "122213", "122312", "132212", "221213",
            "221312", "231212", "112232", "122132", "122231", "113222", "123122", "123221", "223211", "221132",
            "221231", "213212", "223112", "312131", "311222", "321122", "321221", "312212", "322112", "322211",
            "212123", "212321", "232121", "111323", "131123", "131321", "112313", "132113", "132311", "211313",
            "231113", "231311", "112133", "112331", "132131", "113123", "113321", "133121", "313121", "211331",
            "231131", "213113", "213311", "213131", "311123", "311321", "331121", "312113", "312311", "332111",
            "314111", "221411", "431111", "111224", "111422", "121124", "121421", "141122", "141221", "112214",
            "112412", "122114", "122411", "142112", "142211", "241211", "221114", "413111", "241112", "134111",
            "111242", "121142", "121241", "114212", "124112", "124211", "411212", "421112", "421211", "212141",
            "214121", "412121", "111143", "111341", "131141", "114113", "114311", "411113", "411311", "113141",
            "114131", "311141", "411131", "211412", "211214", "211232", "2331112"
    };

    public static String generateSvg(String symbology, String text, String color, String background, boolean showText) {
        if (text == null || text.isEmpty()) return "";
        String sym = symbology != null ? symbology.toLowerCase() : "code128";

        if ("qrcode".equals(sym) || "qr".equals(sym)) {
            return generateQrSvg(text, color, background);
        }
        return generateCode128Svg(text, color, background, showText);
    }

    public static String generateCode128Svg(String text, String color, String background, boolean showText) {
        String barCol = (color == null || color.isEmpty()) ? "#000000" : color;
        String bgCol = (background == null || background.isEmpty() || "transparent".equalsIgnoreCase(background)) ? "none" : background;

        // Start Code B is index 104
        int startCode = 104;
        List<Integer> codes = new ArrayList<>();
        codes.add(startCode);

        int checksum = startCode;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            int code = c - 32;
            if (code >= 0 && code <= 95) {
                codes.add(code);
                checksum += code * (i + 1);
            }
        }

        int checkDigit = checksum % 103;
        codes.add(checkDigit);
        codes.add(106); // Stop code

        StringBuilder pattern = new StringBuilder();
        for (int c : codes) {
            if (c >= 0 && c < CODE128_PATTERNS.length) {
                pattern.append(CODE128_PATTERNS[c]);
            }
        }

        // Render bars
        StringBuilder barsSvg = new StringBuilder();
        double currentX = 10.0;
        double barHeight = showText ? 45.0 : 60.0;

        for (int i = 0; i < pattern.length(); i++) {
            int width = Character.getNumericValue(pattern.charAt(i));
            if (i % 2 == 0) { // Bar
                barsSvg.append(String.format(
                        Locale.US,
                        "<rect x=\"%.1f\" y=\"5\" width=\"%.1f\" height=\"%.1f\" fill=\"%s\" />",
                        currentX, (double) width, barHeight, barCol
                ));
            }
            currentX += width;
        }

        double totalWidth = currentX + 10.0;
        double totalHeight = 70.0;

        String textSvg = "";
        if (showText) {
            textSvg = String.format(
                    Locale.US,
                    "<text x=\"%.1f\" y=\"62\" text-anchor=\"middle\" font-family=\"monospace\" font-size=\"10\" fill=\"%s\">%s</text>",
                    totalWidth / 2.0, barCol, text
            );
        }

        String bgRect = !"none".equals(bgCol)
                ? String.format(Locale.US, "<rect width=\"%.1f\" height=\"%.1f\" fill=\"%s\" />", totalWidth, totalHeight, bgCol)
                : "";

        return String.format(
                Locale.US,
                "<svg viewBox=\"0 0 %.1f %.1f\" preserveAspectRatio=\"none\" style=\"width:100%%;height:100%%;display:block;\" xmlns=\"http://www.w3.org/2000/svg\">%s%s%s</svg>",
                totalWidth, totalHeight, bgRect, barsSvg, textSvg
        );
    }

    public static String generateQrSvg(String text, String color, String background) {
        String barCol = (color == null || color.isEmpty()) ? "#000000" : color;
        String bgCol = (background == null || background.isEmpty() || "transparent".equalsIgnoreCase(background)) ? "none" : background;

        // Generate standard QR-like 21x21 matrix with standard finder patterns
        int size = 21;
        boolean[][] matrix = new boolean[size][size];

        // Finder patterns (top-left, top-right, bottom-left)
        addFinderPattern(matrix, 0, 0);
        addFinderPattern(matrix, size - 7, 0);
        addFinderPattern(matrix, 0, size - 7);

        // Timing patterns
        for (int i = 8; i < size - 8; i++) {
            matrix[6][i] = (i % 2 == 0);
            matrix[i][6] = (i % 2 == 0);
        }

        // Pseudo data encoding hash
        byte[] bytes = text.getBytes();
        int bitIdx = 0;
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (isReserved(r, c, size)) continue;
                int b = bytes[bitIdx % bytes.length] & 0xFF;
                matrix[r][c] = ((b ^ (r * 7 + c * 13)) % 2 == 0);
                bitIdx++;
            }
        }

        StringBuilder rects = new StringBuilder();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                if (matrix[r][c]) {
                    rects.append(String.format(
                            Locale.US,
                            "<rect x=\"%d\" y=\"%d\" width=\"1\" height=\"1\" fill=\"%s\" />",
                            c, r, barCol
                    ));
                }
            }
        }

        String bgRect = !"none".equals(bgCol)
                ? String.format(Locale.US, "<rect width=\"%d\" height=\"%d\" fill=\"%s\" />", size, size, bgCol)
                : "";

        return String.format(
                Locale.US,
                "<svg viewBox=\"0 0 %d %d\" style=\"width:100%%;height:100%%;display:block;\" xmlns=\"http://www.w3.org/2000/svg\">%s%s</svg>",
                size, size, bgRect, rects
        );
    }

    private static void addFinderPattern(boolean[][] m, int startX, int startY) {
        for (int r = 0; r < 7; r++) {
            for (int c = 0; c < 7; c++) {
                boolean isBorder = (r == 0 || r == 6 || c == 0 || c == 6);
                boolean isCenter = (r >= 2 && r <= 4 && c >= 2 && c <= 4);
                m[startY + r][startX + c] = isBorder || isCenter;
            }
        }
    }

    private static boolean isReserved(int r, int c, int size) {
        if (r < 8 && c < 8) return true; // Top-left
        if (r < 8 && c >= size - 8) return true; // Top-right
        if (r >= size - 8 && c < 8) return true; // Bottom-left
        if (r == 6 || c == 6) return true; // Timing
        return false;
    }
}
