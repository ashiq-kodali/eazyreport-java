package io.github.ashiqkodali.eazyreport.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LayoutPage {
    public String id = UUID.randomUUID().toString().substring(0, 8);
    public String name = "Page1";
    public String size = "A4";
    public String orientation = "portrait";
    public double width = 210.0;
    public double height = 297.0;
    public Styles.PageMargins margins = new Styles.PageMargins();
    public double printOffsetX = 0.0;
    public double printOffsetY = 0.0;
    public Styles.Watermark watermark = new Styles.Watermark();

    public Band reportTitle;
    public Band reportSummary;
    public Band pageHeader;
    public Band pageFooter;
    public Band columnHeader;
    public Band columnFooter;
    public Band overlay;

    public List<Band.DataBand> dataBands = new ArrayList<>();
}
