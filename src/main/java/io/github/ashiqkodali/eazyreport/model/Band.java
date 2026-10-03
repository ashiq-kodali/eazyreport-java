package io.github.ashiqkodali.eazyreport.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Band {
    public String id = UUID.randomUUID().toString().substring(0, 8);
    public String type = "band";
    public String name = "";
    public double height = 30.0;
    public boolean canGrow = true;
    public boolean canShrink = false;
    public boolean visible = true;
    public String fill = "";
    public String visibleExpr = "";
    public boolean printOnFirstPage = true;
    public boolean printOnLastPage = true;
    public List<LayoutElement> elements = new ArrayList<>();

    public Band() {}

    public Band(String type, double height) {
        this.type = type;
        this.height = height;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ChildBand extends Band {
        public boolean fillUnusedSpace = false;

        public ChildBand() {
            this.type = "child";
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class GroupBand {
        public String id = UUID.randomUUID().toString().substring(0, 8);
        public String expression = "";
        public Band header;
        public Band footer;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DataBand extends Band {
        public String dataPath = "";
        public String dataset = "";
        public Band header;
        public Band footer;
        public List<GroupBand> groupBands = new ArrayList<>();
        public List<ChildBand> childBands = new ArrayList<>();

        public DataBand() {
            this.type = "data";
        }
    }
}
