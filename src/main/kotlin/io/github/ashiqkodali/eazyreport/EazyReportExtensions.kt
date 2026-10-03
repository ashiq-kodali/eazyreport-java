package io.github.ashiqkodali.eazyreport

import java.io.File
import java.io.InputStream

/**
 * Idiomatic Kotlin DSL builder for EazyReport.
 */
inline fun eazyReport(template: String, block: ReportBuilder.() -> Unit = {}): ReportBuilder {
    return EazyReport.load(template).apply(block)
}

inline fun eazyReport(file: File, block: ReportBuilder.() -> Unit = {}): ReportBuilder {
    return EazyReport.load(file).apply(block)
}

inline fun eazyReport(inputStream: InputStream, block: ReportBuilder.() -> Unit = {}): ReportBuilder {
    return EazyReport.load(inputStream).apply(block)
}

inline fun eazyReport(map: Map<String, Any>, block: ReportBuilder.() -> Unit = {}): ReportBuilder {
    return EazyReport.fromMap(map).apply(block)
}
