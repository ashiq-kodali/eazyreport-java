package io.github.ashiqkodali.eazyreport;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public class EazyReport {

    public static final String VERSION = "1.0.0";

    public static ReportBuilder load(String templateOrPath) throws IOException {
        return ReportBuilder.fromTemplate(templateOrPath);
    }

    public static ReportBuilder load(File file) throws IOException {
        return ReportBuilder.fromFile(file);
    }

    public static ReportBuilder load(InputStream is) throws IOException {
        return ReportBuilder.fromInputStream(is);
    }

    public static ReportBuilder fromMap(Map<String, Object> map) {
        return ReportBuilder.fromMap(map);
    }

    public static String renderHtml(String templateOrPath, Object data, Map<String, Object> params) throws IOException {
        return load(templateOrPath).data(data).params(params).toHtml();
    }

    public static byte[] renderPdf(String templateOrPath, Object data, Map<String, Object> params) throws IOException {
        return load(templateOrPath).data(data).params(params).toPdf();
    }
}
