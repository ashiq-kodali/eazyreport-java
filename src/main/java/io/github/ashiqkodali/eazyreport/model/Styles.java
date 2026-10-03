package io.github.ashiqkodali.eazyreport.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Styles {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TextStyle {
        public String fontFamily = "Inter, sans-serif";
        public double fontSize = 10.0;
        public String color = "#1e293b";
        public String align = "left";
        public String verticalAlign = "top";
        public boolean bold = false;
        public boolean italic = false;
        public boolean underline = false;
        public double lineHeight = 1.2;

        public TextStyle copy() {
            TextStyle s = new TextStyle();
            s.fontFamily = this.fontFamily;
            s.fontSize = this.fontSize;
            s.color = this.color;
            s.align = this.align;
            s.verticalAlign = this.verticalAlign;
            s.bold = this.bold;
            s.italic = this.italic;
            s.underline = this.underline;
            s.lineHeight = this.lineHeight;
            return s;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BoxStyle {
        public String background = "";
        public double borderWidth = 0.0;
        public String borderColor = "";
        public String borderStyle = "solid";
        public double borderRadius = 0.0;
        public double padding = 0.0;
        public Double paddingTop = null;
        public Double paddingRight = null;
        public Double paddingBottom = null;
        public Double paddingLeft = null;

        public double getPaddingTop() { return paddingTop != null ? paddingTop : padding; }
        public double getPaddingRight() { return paddingRight != null ? paddingRight : padding; }
        public double getPaddingBottom() { return paddingBottom != null ? paddingBottom : padding; }
        public double getPaddingLeft() { return paddingLeft != null ? paddingLeft : padding; }

        public BoxStyle copy() {
            BoxStyle b = new BoxStyle();
            b.background = this.background;
            b.borderWidth = this.borderWidth;
            b.borderColor = this.borderColor;
            b.borderStyle = this.borderStyle;
            b.borderRadius = this.borderRadius;
            b.padding = this.padding;
            b.paddingTop = this.paddingTop;
            b.paddingRight = this.paddingRight;
            b.paddingBottom = this.paddingBottom;
            b.paddingLeft = this.paddingLeft;
            return b;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ValueFormat {
        public String type = ""; // text, currency, number, date, percent
        public String currency = "$";
        public int decimals = 2;
        public String pattern = "";
        public boolean thousands = true;

        public ValueFormat() {}

        @JsonCreator
        public static ValueFormat fromJson(Object obj) {
            ValueFormat f = new ValueFormat();
            if (obj instanceof String) {
                f.type = (String) obj;
            } else if (obj instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) obj;
                if (map.containsKey("type")) f.type = String.valueOf(map.get("type"));
                if (map.containsKey("currency")) f.currency = String.valueOf(map.get("currency"));
                if (map.containsKey("decimals") && map.get("decimals") instanceof Number) {
                    f.decimals = ((Number) map.get("decimals")).intValue();
                }
                if (map.containsKey("pattern")) f.pattern = String.valueOf(map.get("pattern"));
                if (map.containsKey("thousands") && map.get("thousands") instanceof Boolean) {
                    f.thousands = (Boolean) map.get("thousands");
                }
            }
            return f;
        }

        public ValueFormat copy() {
            ValueFormat f = new ValueFormat();
            f.type = this.type;
            f.currency = this.currency;
            f.decimals = this.decimals;
            f.pattern = this.pattern;
            f.thousands = this.thousands;
            return f;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PageMargins {
        public double top = 10.0;
        public double bottom = 10.0;
        public double left = 10.0;
        public double right = 10.0;

        public PageMargins() {}

        public PageMargins(double top, double bottom, double left, double right) {
            this.top = top;
            this.bottom = bottom;
            this.left = left;
            this.right = right;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Watermark {
        public boolean enabled = false;
        public String type = "text"; // text or image
        public String text = "";
        public double fontSize = 60.0;
        public String color = "rgba(0, 0, 0, 0.05)";
        public double rotation = -45.0;
        public double opacity = 0.15;
        public String imageSrc = "";
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class LogicRule {
        public String condition = "";
        public Map<String, Object> actions;

        public LogicRule copy() {
            LogicRule r = new LogicRule();
            r.condition = this.condition;
            r.actions = this.actions;
            return r;
        }
    }
}
