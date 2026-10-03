package io.github.ashiqkodali.eazyreport.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;

public class TemplateLoader {
    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static ObjectMapper getMapper() {
        return mapper;
    }

    public static LayoutDocument load(String jsonOrPath) throws IOException {
        String trimmed = jsonOrPath.trim();
        if (trimmed.startsWith("{")) {
            return parseJson(trimmed);
        }

        File file = new File(trimmed);
        if (file.exists() && file.isFile()) {
            return load(file);
        }

        // Try reading as file path directly
        byte[] bytes = Files.readAllBytes(Paths.get(trimmed));
        return parseJson(new String(bytes, StandardCharsets.UTF_8));
    }

    public static LayoutDocument load(File file) throws IOException {
        try (InputStream is = new FileInputStream(file)) {
            return load(is);
        }
    }

    public static LayoutDocument load(InputStream inputStream) throws IOException {
        JsonNode root = mapper.readTree(inputStream);
        return parseJsonNode(root);
    }

    public static LayoutDocument fromMap(Map<String, Object> map) {
        JsonNode node = mapper.valueToTree(map);
        return parseJsonNode(node);
    }

    private static LayoutDocument parseJson(String json) throws IOException {
        JsonNode root = mapper.readTree(json);
        return parseJsonNode(root);
    }

    private static LayoutDocument parseJsonNode(JsonNode root) {
        LayoutDocument doc = new LayoutDocument();
        if (root.has("format")) doc.format = root.get("format").asText();
        if (root.has("version")) doc.version = root.get("version").asInt();
        if (root.has("title")) doc.title = root.get("title").asText();
        if (root.has("author")) doc.author = root.get("author").asText();
        if (root.has("description")) doc.description = root.get("description").asText();

        if (root.has("parameters") && root.get("parameters").isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = root.get("parameters").fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                doc.parameters.put(entry.getKey(), mapper.convertValue(entry.getValue(), Object.class));
            }
        }

        if (root.has("pages") && root.get("pages").isArray()) {
            for (JsonNode pageNode : root.get("pages")) {
                doc.pages.add(parsePage(pageNode));
            }
        }

        if (doc.pages.isEmpty()) {
            doc.pages.add(new LayoutPage());
        }

        return doc;
    }

    private static LayoutPage parsePage(JsonNode node) {
        LayoutPage page = new LayoutPage();
        if (node.has("id")) page.id = node.get("id").asText();
        if (node.has("name")) page.name = node.get("name").asText();
        if (node.has("size")) page.size = node.get("size").asText();
        if (node.has("orientation")) page.orientation = node.get("orientation").asText().toLowerCase();

        boolean isPortrait = !"landscape".equalsIgnoreCase(page.orientation);
        page.width = node.has("width") ? node.get("width").asDouble() : (isPortrait ? 210.0 : 297.0);
        page.height = node.has("height") ? node.get("height").asDouble() : (isPortrait ? 297.0 : 210.0);

        if (node.has("margins")) {
            page.margins = mapper.convertValue(node.get("margins"), Styles.PageMargins.class);
        }
        if (node.has("watermark")) {
            page.watermark = mapper.convertValue(node.get("watermark"), Styles.Watermark.class);
        }

        if (node.has("reportTitle")) page.reportTitle = parseBand(node.get("reportTitle"), "reportTitle");
        if (node.has("reportSummary")) page.reportSummary = parseBand(node.get("reportSummary"), "reportSummary");
        if (node.has("pageHeader")) page.pageHeader = parseBand(node.get("pageHeader"), "pageHeader");
        if (node.has("pageFooter")) page.pageFooter = parseBand(node.get("pageFooter"), "pageFooter");
        if (node.has("columnHeader")) page.columnHeader = parseBand(node.get("columnHeader"), "columnHeader");
        if (node.has("columnFooter")) page.columnFooter = parseBand(node.get("columnFooter"), "columnFooter");
        if (node.has("overlay")) page.overlay = parseBand(node.get("overlay"), "overlay");

        JsonNode dataBandsNode = node.has("dataBands") ? node.get("dataBands") : node.get("data");
        if (dataBandsNode != null) {
            if (dataBandsNode.isArray()) {
                for (JsonNode dbNode : dataBandsNode) {
                    Band.DataBand db = parseDataBand(dbNode);
                    if (db != null) page.dataBands.add(db);
                }
            } else if (dataBandsNode.isObject()) {
                Band.DataBand db = parseDataBand(dataBandsNode);
                if (db != null) page.dataBands.add(db);
            }
        }

        return page;
    }

    private static Band parseBand(JsonNode node, String type) {
        if (node == null || !node.isObject()) return null;
        Band band = mapper.convertValue(node, Band.class);
        band.type = type;
        return band;
    }

    private static Band.DataBand parseDataBand(JsonNode node) {
        if (node == null || !node.isObject()) return null;
        Band.DataBand db = new Band.DataBand();
        if (node.has("id")) db.id = node.get("id").asText();
        if (node.has("name")) db.name = node.get("name").asText();
        if (node.has("height")) db.height = node.get("height").asDouble();
        if (node.has("canGrow")) db.canGrow = node.get("canGrow").asBoolean();
        if (node.has("visible")) db.visible = node.get("visible").asBoolean();
        if (node.has("fill")) db.fill = node.get("fill").asText();
        if (node.has("visibleExpr")) db.visibleExpr = node.get("visibleExpr").asText();
        String dp = "";
        if (node.has("dataPath") && !node.get("dataPath").asText().isEmpty()) dp = node.get("dataPath").asText();
        else if (node.has("arrayPath") && !node.get("arrayPath").asText().isEmpty()) dp = node.get("arrayPath").asText();
        else if (node.has("dataset") && !node.get("dataset").asText().isEmpty()) dp = node.get("dataset").asText();
        db.dataPath = dp;
        db.dataset = dp;

        if (node.has("header")) db.header = parseBand(node.get("header"), "header");
        if (node.has("footer")) db.footer = parseBand(node.get("footer"), "footer");

        if (node.has("elements") && node.get("elements").isArray()) {
            for (JsonNode elNode : node.get("elements")) {
                db.elements.add(mapper.convertValue(elNode, LayoutElement.class));
            }
        }

        if (node.has("childBands") && node.get("childBands").isArray()) {
            for (JsonNode cbNode : node.get("childBands")) {
                Band.ChildBand cb = mapper.convertValue(cbNode, Band.ChildBand.class);
                db.childBands.add(cb);
            }
        }

        return db;
    }
}
