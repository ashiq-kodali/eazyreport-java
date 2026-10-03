package io.github.ashiqkodali.eazyreport.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LayoutElement {
    public String id = UUID.randomUUID().toString().substring(0, 8);
    public String type = "text"; // text, shape, image, barcode, chart, line, checkbox
    public String name = "";
    public double x = 0.0;
    public double y = 0.0;
    public double w = 30.0;
    public double h = 10.0;
    public double opacity = 1.0;
    public boolean visible = true;
    public boolean autoHeight = false;

    // Content & Data
    public String content = "";
    public String text = "";
    public String field = "";
    public String path = "";
    public String binding = "";
    public String value = "";
    public String expr = "";

    public String resolveRawBinding() {
        if (field != null && !field.isEmpty()) return field;
        if (path != null && !path.isEmpty()) return path;
        if (binding != null && !binding.isEmpty()) return binding;
        if (content != null && !content.isEmpty()) return content;
        if (text != null && !text.isEmpty()) return text;
        if (value != null && !value.isEmpty()) return value;
        if (expr != null && !expr.isEmpty()) return expr;
        return "";
    }

    // Styles & Box
    public Styles.TextStyle style = new Styles.TextStyle();
    public Styles.BoxStyle box = new Styles.BoxStyle();
    public Styles.ValueFormat format = new Styles.ValueFormat();

    // Shape / Line
    public String shapeType = "rectangle"; // rectangle, rounded_rect, circle, ellipse, line
    public String fill = "";
    public String stroke = "";
    public double strokeWidth = 1.0;

    // Image
    public String src = "";
    public String fit = "contain"; // contain, cover, fill

    // Barcode / QR
    public String symbology = "code128"; // code128, qrcode, ean13
    public String barcodeValue = "";
    public boolean showText = true;
    public String barColor = "#000000";
    public String barcodeBackground = "transparent";

    // Chart
    public String chartType = "column"; // column, bar, line, area, pie, doughnut
    public String chartTitle = "";
    public String dataset = "";
    public String labelField = "";
    public String valueField = "";

    // Checkbox
    public String checkedExpr = "";

    // Rules
    public List<Styles.LogicRule> rules = new ArrayList<>();

    public LayoutElement copy() {
        LayoutElement el = new LayoutElement();
        el.id = this.id;
        el.type = this.type;
        el.name = this.name;
        el.x = this.x;
        el.y = this.y;
        el.w = this.w;
        el.h = this.h;
        el.opacity = this.opacity;
        el.visible = this.visible;
        el.autoHeight = this.autoHeight;
        el.content = this.content;
        el.text = this.text;
        el.field = this.field;
        el.path = this.path;
        el.binding = this.binding;
        el.value = this.value;
        el.expr = this.expr;
        el.style = this.style.copy();
        el.box = this.box.copy();
        el.format = this.format.copy();
        el.shapeType = this.shapeType;
        el.fill = this.fill;
        el.stroke = this.stroke;
        el.strokeWidth = this.strokeWidth;
        el.src = this.src;
        el.fit = this.fit;
        el.symbology = this.symbology;
        el.barcodeValue = this.barcodeValue;
        el.showText = this.showText;
        el.barColor = this.barColor;
        el.barcodeBackground = this.barcodeBackground;
        el.chartType = this.chartType;
        el.chartTitle = this.chartTitle;
        el.dataset = this.dataset;
        el.labelField = this.labelField;
        el.valueField = this.valueField;
        el.checkedExpr = this.checkedExpr;
        for (Styles.LogicRule r : this.rules) {
            el.rules.add(r.copy());
        }
        return el;
    }
}
