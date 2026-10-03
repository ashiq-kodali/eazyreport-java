<p align="center">
  <img src="assets/eazyreport_banner.png" alt="EazyReport Banner" width="100%" />
</p>

<h1 align="center">EazyReport for Java & Kotlin</h1>

<p align="center">
  <strong>High-performance, pure JVM report generation engine supporting visual <code>.rtpl</code> templates designed at <a href="https://eazyreport.in/">eazyreport.in</a>.</strong>
</p>

<p align="center">
  <a href="https://github.com/ashiq-kodali/eazyreport-java/actions"><img src="https://img.shields.io/github/actions/workflow/status/ashiq-kodali/eazyreport-java/tests.yml?branch=main&label=tests" alt="Tests" /></a>
  <a href="https://search.maven.org/artifact/io.github.ashiq-kodali/eazyreport"><img src="https://img.shields.io/maven-central/v/io.github.ashiq-kodali/eazyreport" alt="Maven Central" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="License" /></a>
  <a href="https://openjdk.org"><img src="https://img.shields.io/badge/Java-17%20%7C%2021-orange.svg" alt="Java Version" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-Compatible-7F52FF.svg" alt="Kotlin" /></a>
</p>

---

## 🌟 Overview

**EazyReport** is an enterprise report engine built for Java 17+ and Kotlin (Spring Boot, Quarkus, Micronaut, Android, and standalone JVM apps). It allows developers to bind Java POJOs, Records, DTOs, Maps, and Spring collections directly into visual templates (`.rtpl`) created with the drag-and-drop web designer at [https://eazyreport.in/](https://eazyreport.in/).

Generate pixel-perfect **HTML**, **print previews**, and vector **PDF** documents with zero external browser runtimes or heavy C libraries.

---

## 🚀 Key Features

- **Visual Designer Ready**: Natively load and execute `.rtpl` (v2) template files designed on [https://eazyreport.in/](https://eazyreport.in/).
- **Multi-Band Layout Architecture**: Supports Report Title, Report Summary, Page Header, Page Footer, Column Header, Column Footer, Data Bands, and Child Bands with automatic multi-page pagination.
- **Dynamic Content Growth**: Automatically measures and expands text boxes (`autoHeight`, `canGrow`) across page breaks while keeping headers and footers intact.
- **Built-in Barcode & 2D QR Code Generation**: Pure vector SVG barcode generation (Code 128, QR Code) without requiring heavy C extensions.
- **Pure SVG Charting**: Embed beautiful column, bar, line, area, pie, and doughnut charts directly in your reports.
- **Robust Expression Engine**: Full Handlebars syntax (`{{field}}`, `{{#if}}`, `{{#each}}`), 35+ formatting helpers (`currency`, `number`, `words`, `date`), and arithmetic formula evaluation.
- **Zero-Dependency Vector PDF**: Generates valid PDF 1.4 documents out of the box in pure Java byte streams.
- **Kotlin DSL & Extensions**: First-class idiomatic builder syntax for Kotlin developers.

---

## 📦 Installation

### Maven

Add the dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.ashiq-kodali</groupId>
    <artifactId>eazyreport</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle (Kotlin DSL)

```kotlin
implementation("io.github.ashiq-kodali:eazyreport:1.0.0")
```

### Gradle (Groovy)

```groovy
implementation 'io.github.ashiq-kodali:eazyreport:1.0.0'
```

---

## ⚡ Quick Start

### 1. Java (Spring Boot / Standard JVM)

```java
import io.github.ashiqkodali.eazyreport.EazyReport;
import io.github.ashiqkodali.eazyreport.ReportBuilder;

import java.io.File;
import java.util.*;

public class Main {
    public static void main(String[] args) throws Exception {
        // 1. Prepare data payload
        Map<String, Object> data = new HashMap<>();
        
        Map<String, Object> invoice = new HashMap<>();
        invoice.put("number", "INV-2026-001");
        invoice.put("date", "2026-10-03");
        invoice.put("dueDate", "2026-10-17");
        data.put("invoice", invoice);

        Map<String, Object> customer = new HashMap<>();
        customer.put("name", "Acme Global Solutions");
        customer.put("address", "100 Innovation Way, Suite 400");
        data.put("customer", customer);

        List<Map<String, Object>> items = List.of(
            Map.of("description", "Enterprise Cloud Subscription", "qty", 10, "rate", 150.0, "amount", 1500.0),
            Map.of("description", "Dedicated Migration Support", "qty", 20, "rate", 85.0, "amount", 1700.0)
        );
        data.put("items", items);

        // 2. Load template and bind data
        ReportBuilder builder = EazyReport.load(new File("reports/invoice.rtpl"))
            .data(data)
            .param("currency", "USD");

        // 3. Render HTML or PDF
        String html = builder.toHtml();
        byte[] pdfBytes = builder.toPdf();

        // 4. Save to disk or stream in controller
        builder.savePdf(new File("output/invoice.pdf"));
    }
}
```

---

### 2. Spring Boot Controller Example

```java
package com.example.demo;

import io.github.ashiqkodali.eazyreport.EazyReport;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    @GetMapping("/{id}/pdf")
    public ResponseEntity<ByteArrayResource> downloadInvoice(@PathVariable String id) throws Exception {
        Order order = orderService.findById(id);

        byte[] pdf = EazyReport.load("templates/invoice.rtpl")
            .data(order)
            .param("printedBy", "System")
            .toPdf();

        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice-" + id + ".pdf\"")
            .body(new ByteArrayResource(pdf));
    }
}
```

---

### 3. Kotlin Idiomatic DSL

```kotlin
import io.github.ashiqkodali.eazyreport.eazyReport
import java.io.File

fun generateReport() {
    val pdfBytes = eazyReport(File("reports/invoice.rtpl")) {
        data(mapOf(
            "invoice" to mapOf("number" to "INV-2026-999"),
            "items" to listOf(
                mapOf("description" to "Kotlin Native Service", "amount" to 500.0)
            )
        ))
        param("currency", "EUR")
    }.toPdf()

    File("output/invoice.pdf").writeBytes(pdfBytes)
}
```

---

## 💼 Real-World Use Cases

| Use Case | Description | Highlights |
| :--- | :--- | :--- |
| **Commercial Invoicing & Billing** | Multi-item billing with automated tax calculation, currency formatting, and currency amounts in words. | `{{words total}}`, `{{currency amount}}`, Code 128 barcodes |
| **Logistics & Shipping Manifests** | High-density shipping waybills with parcel barcodes and 2D QR codes for instant warehouse scanning. | Native QR codes, fixed-height rows, fill-unused-space bands |
| **Executive & Financial Dashboards** | Management reports complete with revenue summaries and embedded vector charts. | SVG Column, Bar, Line, and Doughnut charts |
| **Academic & Training Certificates** | Landscape-oriented credential certificates with custom typography and subtle background watermarks. | Landscape orientation, dynamic watermarks, vector shapes |

---

## 🧮 Expressions & Helper Reference

EazyReport supports rich Handlebars expressions and over 35 built-in helper functions:

```handlebars
{{! Formatting Helpers }}
{{currency total '$' 2}}           <!-- $1,234.50 -->
{{number count 0}}                <!-- 1,000 -->
{{words total}}                   <!-- One Thousand Two Hundred Thirty-Four and 50/100 -->
{{date createdAt 'yyyy-MM-dd'}}   <!-- 2026-10-03 -->

{{! Text Helpers }}
{{upper customer.name}}           <!-- ACME CORP -->
{{lower status}}                  <!-- pending -->
{{capitalize title}}              <!-- Monthly Report -->

{{! Logic & Control Flow }}
{{#if isOverdue}}
    <span style="color: red;">PAYMENT OVERDUE</span>
{{/if}}

{{#each items}}
    Row: {{this.description}} - {{this.amount}}
{{/each}}
```

---

## 🎨 Visual Template Designer

Templates can be visually created and edited in your web browser:

👉 **[https://eazyreport.in/](https://eazyreport.in/)**

Design layouts with drag-and-drop precision, configure data bindings, style bands, and export `.rtpl` files ready for use with EazyReport in Java, Kotlin, PHP, Python, .NET, Flutter, and more!

---

## 👥 Contributors

- **Ashiq Kodali** ([@ashiq-kodali](https://github.com/ashiq-kodali))
- **Thameem PK**

---

## 📄 License

This package is open-sourced software licensed under the [MIT License](LICENSE).
