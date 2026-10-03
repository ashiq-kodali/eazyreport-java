package io.github.ashiqkodali.eazyreport.engine;

import io.github.ashiqkodali.eazyreport.expressions.ExpressionEngine;
import java.util.HashMap;
import java.util.Map;

public class RenderCtx {
    public Object data;
    public Object item;
    public Map<String, Object> params = new HashMap<>();
    public int pageNumber = 1;
    public int totalPages = 1;

    public Object resolveValue(String path) {
        if (path == null || path.isEmpty()) return null;

        Map<String, Object> ctx = BandLayout.buildContextObject(this);
        Object val = ExpressionEngine.getPath(ctx, path);
        if (val != null) return val;

        // Try stripping "item." or "data." prefix if present
        if (path.startsWith("item.") && item != null) {
            return ExpressionEngine.getPath(item, path.substring(5));
        }
        if (path.startsWith("data.") && data != null) {
            return ExpressionEngine.getPath(data, path.substring(5));
        }

        return null;
    }

    public RenderCtx copy() {
        RenderCtx ctx = new RenderCtx();
        ctx.data = this.data;
        ctx.item = this.item;
        ctx.params = new HashMap<>(this.params);
        ctx.pageNumber = this.pageNumber;
        ctx.totalPages = this.totalPages;
        return ctx;
    }
}
