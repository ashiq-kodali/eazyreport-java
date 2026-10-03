package io.github.ashiqkodali.eazyreport.expressions;

public class NumberToWords {
    private static final String[] ONES = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen"
    };

    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    private static final String[] THOUSANDS = {
            "", "Thousand", "Million", "Billion", "Trillion"
    };

    public static String convert(double number) {
        if (Double.isNaN(number) || Double.isInfinite(number)) {
            return "";
        }

        long whole = (long) Math.floor(Math.abs(number));
        long cents = Math.round((Math.abs(number) - whole) * 100.0);

        String words = whole == 0 ? "Zero" : convertWholeNumber(whole);
        String prefix = number < 0 ? "Negative " : "";

        return String.format("%s%s and %02d/100", prefix, words, cents);
    }

    private static String convertWholeNumber(long number) {
        if (number == 0) return "Zero";

        StringBuilder sb = new StringBuilder();
        int groupIndex = 0;

        while (number > 0) {
            int chunk = (int) (number % 1000);
            if (chunk != 0) {
                String chunkWords = convertChunk(chunk);
                String scale = THOUSANDS[groupIndex];
                if (scale.isEmpty()) {
                    sb.insert(0, chunkWords);
                } else {
                    sb.insert(0, chunkWords + " " + scale + (sb.length() > 0 ? " " : ""));
                }
            }
            number /= 1000;
            groupIndex++;
        }

        return sb.toString().trim();
    }

    private static String convertChunk(int number) {
        StringBuilder sb = new StringBuilder();

        if (number >= 100) {
            sb.append(ONES[number / 100]).append(" Hundred");
            number %= 100;
            if (number > 0) sb.append(" ");
        }

        if (number >= 20) {
            sb.append(TENS[number / 10]);
            number %= 10;
            if (number > 0) {
                sb.append("-").append(ONES[number]);
            }
        } else if (number > 0) {
            sb.append(ONES[number]);
        }

        return sb.toString();
    }
}
