package io.github.ashiqkodali.eazyreport.engine;

import io.github.ashiqkodali.eazyreport.model.Styles;
import java.util.List;

public class RenderedPage {
    public String id;
    public double width;
    public double height;
    public String html;
    public List<BandLayout.PlacedBandRecord> placedBands;
    public Styles.Watermark watermark;
    public Styles.PageMargins margins;

    public RenderedPage(String id, double width, double height, String html,
                        List<BandLayout.PlacedBandRecord> placedBands,
                        Styles.Watermark watermark, Styles.PageMargins margins) {
        this.id = id;
        this.width = width;
        this.height = height;
        this.html = html;
        this.placedBands = placedBands;
        this.watermark = watermark;
        this.margins = margins;
    }
}
