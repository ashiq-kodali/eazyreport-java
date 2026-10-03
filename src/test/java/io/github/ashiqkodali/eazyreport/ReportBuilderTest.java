package io.github.ashiqkodali.eazyreport;

import io.github.ashiqkodali.eazyreport.engine.BarcodeGenerator;
import io.github.ashiqkodali.eazyreport.engine.ChartGenerator;
import io.github.ashiqkodali.eazyreport.engine.RenderedPage;
import io.github.ashiqkodali.eazyreport.model.LayoutDocument;
import io.github.ashiqkodali.eazyreport.model.LayoutElement;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ReportBuilderTest {

    @Test
    public void testLoadAndRenderInvoice() throws Exception {
        InputStream is = getClass().getResourceAsStream("/test_invoice.rtpl");
        assertNotNull(is, "test_invoice.rtpl resource must exist");

        ReportBuilder builder = EazyReport.load(is);
        LayoutDocument doc = builder.getDocument();
        assertTrue(doc.title.contains("Commercial Invoice"));
        assertFalse(doc.pages.isEmpty());

        Map<String, Object> data = new HashMap<>();
        Map<String, Object> invoice = new HashMap<>();
        invoice.put("number", "INV-JAVA-2026");
        invoice.put("date", "2026-10-03");
        data.put("invoice", invoice);

        Map<String, Object> customer = new HashMap<>();
        customer.put("name", "Enterprise Java Systems");
        customer.put("address", "100 Oracle Parkway");
        data.put("customer", customer);

        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> i1 = new HashMap<>();
        i1.put("description", "JVM Microservices Cluster");
        i1.put("qty", 5);
        i1.put("rate", 200.0);
        i1.put("amount", 1000.0);
        items.add(i1);

        Map<String, Object> i2 = new HashMap<>();
        i2.put("description", "Enterprise Support 24/7");
        i2.put("qty", 10);
        i2.put("rate", 80.0);
        i2.put("amount", 800.0);
        items.add(i2);

        data.put("items", items);

        builder.data(data);

        List<RenderedPage> pages = builder.getPages();
        assertFalse(pages.isEmpty());
        assertEquals(210.0, pages.get(0).width);
        assertEquals(297.0, pages.get(0).height);

        // Test HTML output
        String html = builder.toHtml();
        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("INV-JAVA-2026"));
        assertTrue(html.contains("Enterprise Java Systems"));
        assertTrue(html.contains("JVM Microservices Cluster"));

        // Test PDF output
        byte[] pdf = builder.toPdf();
        assertNotNull(pdf);
        assertTrue(pdf.length > 100);
        String pdfHeader = new String(pdf, 0, Math.min(10, pdf.length), StandardCharsets.ISO_8859_1);
        assertTrue(pdfHeader.startsWith("%PDF-1.4"));
    }

    @Test
    public void testBarcodeAndChartGeneration() {
        // Code 128
        String barcode = BarcodeGenerator.generateSvg("code128", "PKG-12345", "#000000", "#ffffff", true);
        assertTrue(barcode.contains("<svg"));
        assertTrue(barcode.contains("rect"));

        // QR Code
        String qr = BarcodeGenerator.generateSvg("qrcode", "https://eazyreport.in", "#000000", "#ffffff", false);
        assertTrue(qr.contains("<svg"));
        assertTrue(qr.contains("rect"));

        // Chart
        LayoutElement el = new LayoutElement();
        el.chartType = "column";
        el.chartTitle = "Revenue by Region";
        el.w = 120.0;
        el.h = 60.0;

        String chart = ChartGenerator.generateSvg(el, Arrays.asList("North", "South", "East", "West"), Arrays.asList(40.0, 60.0, 55.0, 90.0));
        assertTrue(chart.contains("<svg"));
        assertTrue(chart.contains("Revenue by Region"));
    }
}
