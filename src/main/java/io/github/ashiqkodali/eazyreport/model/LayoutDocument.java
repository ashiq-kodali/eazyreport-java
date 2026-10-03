package io.github.ashiqkodali.eazyreport.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class LayoutDocument {
    public String format = "report-designer-layout";
    public int version = 2;
    public String title = "Report";
    public String author = "";
    public String description = "";
    public List<LayoutPage> pages = new ArrayList<>();
    public List<Object> datasets = new ArrayList<>();
    public Map<String, Object> parameters = new HashMap<>();
    public Map<String, Object> styles = new HashMap<>();
}
