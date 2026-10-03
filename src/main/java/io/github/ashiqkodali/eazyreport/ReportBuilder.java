package io.github.ashiqkodali.eazyreport;

import io.github.ashiqkodali.eazyreport.engine.PaginationEngine;
import io.github.ashiqkodali.eazyreport.engine.RenderedPage;
import io.github.ashiqkodali.eazyreport.model.LayoutDocument;
import io.github.ashiqkodali.eazyreport.model.TemplateLoader;
import io.github.ashiqkodali.eazyreport.renderers.HtmlRenderer;
import io.github.ashiqkodali.eazyreport.renderers.PdfRenderer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReportBuilder {

    private final LayoutDocument document;
    private Object data;
    private final Map<String, Object> parameters = new HashMap<>();
    private final Map<String, Object> options = new HashMap<>();
    private List<RenderedPage> renderedPages;

    public ReportBuilder(LayoutDocument document) {
        this.document = document;
    }

    public static ReportBuilder fromTemplate(String templateOrPath) throws IOException {
        LayoutDocument doc = TemplateLoader.load(templateOrPath);
        return new ReportBuilder(doc);
    }

    public static ReportBuilder fromFile(File file) throws IOException {
        LayoutDocument doc = TemplateLoader.load(file);
        return new ReportBuilder(doc);
    }

    public static ReportBuilder fromInputStream(InputStream is) throws IOException {
        LayoutDocument doc = TemplateLoader.load(is);
        return new ReportBuilder(doc);
    }

    public static ReportBuilder fromMap(Map<String, Object> map) {
        LayoutDocument doc = TemplateLoader.fromMap(map);
        return new ReportBuilder(doc);
    }

    public ReportBuilder data(Object data) {
        this.data = data;
        this.renderedPages = null;
        return this;
    }

    public ReportBuilder params(Map<String, Object> params) {
        if (params != null) {
            this.parameters.putAll(params);
        }
        this.renderedPages = null;
        return this;
    }

    public ReportBuilder param(String key, Object value) {
        this.parameters.put(key, value);
        this.renderedPages = null;
        return this;
    }

    public ReportBuilder options(Map<String, Object> options) {
        if (options != null) {
            this.options.putAll(options);
        }
        this.renderedPages = null;
        return this;
    }

    public List<RenderedPage> getPages() {
        if (this.renderedPages == null) {
            this.renderedPages = PaginationEngine.paginate(
                    this.document,
                    this.data,
                    this.parameters,
                    this.options
            );
        }
        return this.renderedPages;
    }

    public int getPageCount() {
        return getPages().size();
    }

    public String toHtml() {
        return HtmlRenderer.render(getPages(), this.options);
    }

    public String toHtml(Map<String, Object> renderOptions) {
        Map<String, Object> merged = new HashMap<>(this.options);
        if (renderOptions != null) merged.putAll(renderOptions);
        return HtmlRenderer.render(getPages(), merged);
    }

    public byte[] toPdf() {
        return PdfRenderer.render(getPages(), this.options);
    }

    public byte[] toPdf(Map<String, Object> renderOptions) {
        Map<String, Object> merged = new HashMap<>(this.options);
        if (renderOptions != null) merged.putAll(renderOptions);
        return PdfRenderer.render(getPages(), merged);
    }

    public void saveHtml(File file) throws IOException {
        String html = toHtml();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(html.getBytes(StandardCharsets.UTF_8));
        }
    }

    public void savePdf(File file) throws IOException {
        byte[] pdf = toPdf();
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(pdf);
        }
    }

    public LayoutDocument getDocument() {
        return this.document;
    }
}
