package io.github.ashiqkodali.eazyreport.engine;

import io.github.ashiqkodali.eazyreport.expressions.ExpressionEngine;
import io.github.ashiqkodali.eazyreport.model.Band;
import io.github.ashiqkodali.eazyreport.model.LayoutDocument;
import io.github.ashiqkodali.eazyreport.model.LayoutPage;
import io.github.ashiqkodali.eazyreport.model.Styles;

import java.util.*;

public class PaginationEngine {

    public static List<RenderedPage> paginate(LayoutDocument doc, Object data, Map<String, Object> parameters, Map<String, Object> options) {
        Map<String, Object> allParams = new HashMap<>();
        if (doc.parameters != null) allParams.putAll(doc.parameters);
        if (parameters != null) allParams.putAll(parameters);
        if (options != null && options.containsKey("params") && options.get("params") instanceof Map) {
            allParams.putAll((Map<String, Object>) options.get("params"));
        }

        int maxPages = options != null && options.containsKey("maxPages") ? ((Number) options.get("maxPages")).intValue() : 2000;

        // Pass 1: compute page count
        List<RenderedPage> pass1 = executePaginationPass(doc, data, allParams, 1, maxPages);
        int totalPages = Math.max(1, pass1.size());

        // Pass 2: layout with accurate total pages
        return executePaginationPass(doc, data, allParams, totalPages, maxPages);
    }

    private static List<RenderedPage> executePaginationPass(LayoutDocument doc, Object data, Map<String, Object> parameters, int totalPages, int maxPages) {
        List<RenderedPage> pages = new ArrayList<>();
        if (doc.pages == null || doc.pages.isEmpty()) return pages;

        LayoutPage pageTpl = doc.pages.get(0);
        double pw = pageTpl.width;
        double ph = pageTpl.height;
        Styles.PageMargins margins = pageTpl.margins != null ? pageTpl.margins : new Styles.PageMargins();
        double bodyW = pw - margins.left - margins.right;
        double bodyH = ph - margins.top - margins.bottom;

        double footerH = pageTpl.pageFooter != null ? pageTpl.pageFooter.height : 0.0;
        double maxBodyY = margins.top + bodyH - footerH;

        final int[] pageNum = {1};
        final double[] currentY = {margins.top};
        final List<BandLayout.PlacedBandRecord> currentPlacedBands = new ArrayList<>();
        final StringBuilder currentBandHtml = new StringBuilder();

        Runnable flushPage = () -> {
            if (pageTpl.pageFooter != null) {
                RenderCtx footerRc = new RenderCtx();
                footerRc.data = data;
                footerRc.params = parameters;
                footerRc.pageNumber = pageNum[0];
                footerRc.totalPages = totalPages;

                double fy = ph - margins.bottom - pageTpl.pageFooter.height;
                BandLayout.LaidOutBand laidFooter = BandLayout.layout(pageTpl.pageFooter, footerRc, bodyW, "", pageTpl.pageFooter.height);
                if (laidFooter != null) {
                    currentPlacedBands.add(new BandLayout.PlacedBandRecord(
                            pageTpl.pageFooter, margins.left, fy, bodyW, laidFooter.height, pageTpl.pageFooter.fill, laidFooter.elements
                    ));
                    currentBandHtml.append(String.format(Locale.US,
                            "<div style=\"position:absolute;left:%.2fmm;top:%.2fmm;width:%.2fmm;height:%.2fmm;\">%s</div>",
                            margins.left, fy, bodyW, laidFooter.height, laidFooter.html));
                }
            }

            pages.add(new RenderedPage(
                    "page-" + pageNum[0],
                    pw,
                    ph,
                    currentBandHtml.toString(),
                    new ArrayList<>(currentPlacedBands),
                    pageTpl.watermark != null ? pageTpl.watermark : new Styles.Watermark(),
                    margins
            ));

            currentPlacedBands.clear();
            currentBandHtml.setLength(0);
            pageNum[0]++;
            currentY[0] = margins.top;
        };

        Runnable startNewPage = () -> {
            flushPage.run();

            if (pageTpl.pageHeader != null) {
                RenderCtx headerRc = new RenderCtx();
                headerRc.data = data;
                headerRc.params = parameters;
                headerRc.pageNumber = pageNum[0];
                headerRc.totalPages = totalPages;

                BandLayout.LaidOutBand laidHeader = BandLayout.layout(pageTpl.pageHeader, headerRc, bodyW, "", null);
                if (laidHeader != null) {
                    currentPlacedBands.add(new BandLayout.PlacedBandRecord(
                            pageTpl.pageHeader, margins.left, currentY[0], bodyW, laidHeader.height, pageTpl.pageHeader.fill, laidHeader.elements
                    ));
                    currentBandHtml.append(String.format(Locale.US,
                            "<div style=\"position:absolute;left:%.2fmm;top:%.2fmm;width:%.2fmm;height:%.2fmm;\">%s</div>",
                            margins.left, currentY[0], bodyW, laidHeader.height, laidHeader.html));
                    currentY[0] += laidHeader.height;
                }
            }
        };

        // 1. ReportTitle
        if (pageTpl.reportTitle != null && pageTpl.reportTitle.printOnFirstPage) {
            RenderCtx titleRc = new RenderCtx();
            titleRc.data = data;
            titleRc.params = parameters;
            titleRc.pageNumber = pageNum[0];
            titleRc.totalPages = totalPages;

            BandLayout.LaidOutBand laidTitle = BandLayout.layout(pageTpl.reportTitle, titleRc, bodyW, "", null);
            if (laidTitle != null) {
                currentPlacedBands.add(new BandLayout.PlacedBandRecord(
                        pageTpl.reportTitle, margins.left, currentY[0], bodyW, laidTitle.height, pageTpl.reportTitle.fill, laidTitle.elements
                ));
                currentBandHtml.append(String.format(Locale.US,
                        "<div style=\"position:absolute;left:%.2fmm;top:%.2fmm;width:%.2fmm;height:%.2fmm;\">%s</div>",
                        margins.left, currentY[0], bodyW, laidTitle.height, laidTitle.html));
                currentY[0] += laidTitle.height;
            }
        }

        // 2. PageHeader (first page)
        if (pageTpl.pageHeader != null && pageTpl.pageHeader.printOnFirstPage) {
            RenderCtx headerRc = new RenderCtx();
            headerRc.data = data;
            headerRc.params = parameters;
            headerRc.pageNumber = pageNum[0];
            headerRc.totalPages = totalPages;

            BandLayout.LaidOutBand laidHeader = BandLayout.layout(pageTpl.pageHeader, headerRc, bodyW, "", null);
            if (laidHeader != null) {
                currentPlacedBands.add(new BandLayout.PlacedBandRecord(
                        pageTpl.pageHeader, margins.left, currentY[0], bodyW, laidHeader.height, pageTpl.pageHeader.fill, laidHeader.elements
                ));
                currentBandHtml.append(String.format(Locale.US,
                        "<div style=\"position:absolute;left:%.2fmm;top:%.2fmm;width:%.2fmm;height:%.2fmm;\">%s</div>",
                        margins.left, currentY[0], bodyW, laidHeader.height, laidHeader.html));
                currentY[0] += laidHeader.height;
            }
        }

        // 3. DataBands
        for (Band.DataBand dataBand : pageTpl.dataBands) {
            List<Object> records = resolveRecords(dataBand, data);

            // DataHeader
            if (dataBand.header != null) {
                RenderCtx dhRc = new RenderCtx();
                dhRc.data = data;
                dhRc.params = parameters;
                dhRc.pageNumber = pageNum[0];
                dhRc.totalPages = totalPages;

                BandLayout.LaidOutBand laidDh = BandLayout.layout(dataBand.header, dhRc, bodyW, "", null);
                if (laidDh != null) {
                    if (currentY[0] + laidDh.height > maxBodyY) {
                        startNewPage.run();
                    }

                    currentPlacedBands.add(new BandLayout.PlacedBandRecord(
                            dataBand.header, margins.left, currentY[0], bodyW, laidDh.height, dataBand.header.fill, laidDh.elements
                    ));
                    currentBandHtml.append(String.format(Locale.US,
                            "<div style=\"position:absolute;left:%.2fmm;top:%.2fmm;width:%.2fmm;height:%.2fmm;\">%s</div>",
                            margins.left, currentY[0], bodyW, laidDh.height, laidDh.html));
                    currentY[0] += laidDh.height;
                }
            }

            // Records
            for (Object item : records) {
                RenderCtx itemRc = new RenderCtx();
                itemRc.data = data;
                itemRc.item = item;
                itemRc.params = parameters;
                itemRc.pageNumber = pageNum[0];
                itemRc.totalPages = totalPages;

                BandLayout.LaidOutBand laidItem = BandLayout.layout(dataBand, itemRc, bodyW, "", null);
                if (laidItem != null) {
                    if (currentY[0] + laidItem.height > maxBodyY) {
                        startNewPage.run();
                    }

                    currentPlacedBands.add(new BandLayout.PlacedBandRecord(
                            dataBand, margins.left, currentY[0], bodyW, laidItem.height, dataBand.fill, laidItem.elements
                    ));
                    currentBandHtml.append(String.format(Locale.US,
                            "<div style=\"position:absolute;left:%.2fmm;top:%.2fmm;width:%.2fmm;height:%.2fmm;\">%s</div>",
                            margins.left, currentY[0], bodyW, laidItem.height, laidItem.html));
                    currentY[0] += laidItem.height;
                }

                // ChildBands
                for (Band.ChildBand cb : dataBand.childBands) {
                    if (cb.fillUnusedSpace) continue;

                    BandLayout.LaidOutBand laidCb = BandLayout.layout(cb, itemRc, bodyW, "", null);
                    if (laidCb != null) {
                        if (currentY[0] + laidCb.height > maxBodyY) {
                            startNewPage.run();
                        }

                        currentPlacedBands.add(new BandLayout.PlacedBandRecord(
                                cb, margins.left, currentY[0], bodyW, laidCb.height, cb.fill, laidCb.elements
                        ));
                        currentBandHtml.append(String.format(Locale.US,
                                "<div style=\"position:absolute;left:%.2fmm;top:%.2fmm;width:%.2fmm;height:%.2fmm;\">%s</div>",
                                margins.left, currentY[0], bodyW, laidCb.height, laidCb.html));
                        currentY[0] += laidCb.height;
                    }
                }
            }

            // FillUnusedSpace
            for (Band.ChildBand cb : dataBand.childBands) {
                if (!cb.fillUnusedSpace || cb.height <= 0) continue;

                RenderCtx blankRc = new RenderCtx();
                blankRc.data = data;
                blankRc.params = parameters;
                blankRc.pageNumber = pageNum[0];
                blankRc.totalPages = totalPages;

                while (currentY[0] + cb.height <= maxBodyY) {
                    BandLayout.LaidOutBand laidFill = BandLayout.layout(cb, blankRc, bodyW, "", null);
                    if (laidFill == null) break;

                    currentPlacedBands.add(new BandLayout.PlacedBandRecord(
                            cb, margins.left, currentY[0], bodyW, laidFill.height, cb.fill, laidFill.elements
                    ));
                    currentBandHtml.append(String.format(Locale.US,
                            "<div style=\"position:absolute;left:%.2fmm;top:%.2fmm;width:%.2fmm;height:%.2fmm;\">%s</div>",
                            margins.left, currentY[0], bodyW, laidFill.height, laidFill.html));
                    currentY[0] += laidFill.height;
                }
            }

            // DataFooter
            if (dataBand.footer != null) {
                RenderCtx dfRc = new RenderCtx();
                dfRc.data = data;
                dfRc.params = parameters;
                dfRc.pageNumber = pageNum[0];
                dfRc.totalPages = totalPages;

                BandLayout.LaidOutBand laidDf = BandLayout.layout(dataBand.footer, dfRc, bodyW, "", null);
                if (laidDf != null) {
                    if (currentY[0] + laidDf.height > maxBodyY) {
                        startNewPage.run();
                    }

                    currentPlacedBands.add(new BandLayout.PlacedBandRecord(
                            dataBand.footer, margins.left, currentY[0], bodyW, laidDf.height, dataBand.footer.fill, laidDf.elements
                    ));
                    currentBandHtml.append(String.format(Locale.US,
                            "<div style=\"position:absolute;left:%.2fmm;top:%.2fmm;width:%.2fmm;height:%.2fmm;\">%s</div>",
                            margins.left, currentY[0], bodyW, laidDf.height, laidDf.html));
                    currentY[0] += laidDf.height;
                }
            }
        }

        // 4. ReportSummary
        if (pageTpl.reportSummary != null) {
            RenderCtx sumRc = new RenderCtx();
            sumRc.data = data;
            sumRc.params = parameters;
            sumRc.pageNumber = pageNum[0];
            sumRc.totalPages = totalPages;

            BandLayout.LaidOutBand laidSum = BandLayout.layout(pageTpl.reportSummary, sumRc, bodyW, "", null);
            if (laidSum != null) {
                if (currentY[0] + laidSum.height > maxBodyY) {
                    startNewPage.run();
                }

                currentPlacedBands.add(new BandLayout.PlacedBandRecord(
                        pageTpl.reportSummary, margins.left, currentY[0], bodyW, laidSum.height, pageTpl.reportSummary.fill, laidSum.elements
                ));
                currentBandHtml.append(String.format(Locale.US,
                        "<div style=\"position:absolute;left:%.2fmm;top:%.2fmm;width:%.2fmm;height:%.2fmm;\">%s</div>",
                        margins.left, currentY[0], bodyW, laidSum.height, laidSum.html));
                currentY[0] += laidSum.height;
            }
        }

        flushPage.run();
        return pages;
    }

    private static List<Object> resolveRecords(Band.DataBand band, Object data) {
        if (data == null) return Collections.emptyList();

        String path = (band.dataPath != null && !band.dataPath.isEmpty()) ? band.dataPath : band.dataset;
        Object list = (path != null && !path.isEmpty()) ? ExpressionEngine.getPath(data, path) : data;

        if (list instanceof List) {
            return (List<Object>) list;
        }

        if (list instanceof Iterable) {
            List<Object> res = new ArrayList<>();
            for (Object obj : (Iterable<?>) list) res.add(obj);
            return res;
        }

        if (list != null) {
            return Collections.singletonList(list);
        }

        return Collections.emptyList();
    }
}
