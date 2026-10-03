package io.github.ashiqkodali.eazyreport;

import io.github.ashiqkodali.eazyreport.expressions.ExpressionEngine;
import io.github.ashiqkodali.eazyreport.expressions.Formatters;
import io.github.ashiqkodali.eazyreport.expressions.NumberToWords;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class ExpressionsTest {

    @Test
    public void testNumberToWords() {
        assertEquals("Twelve Thousand Three Hundred Forty-Five and 67/100", NumberToWords.convert(12345.67));
        assertEquals("Zero and 00/100", NumberToWords.convert(0));
        assertEquals("One Million and 00/100", NumberToWords.convert(1000000));
    }

    @Test
    public void testFormatters() {
        assertEquals("$1,234.50", Formatters.formatCurrency(1234.5, "$", 2));
        assertEquals("€500.00", Formatters.formatCurrency(500, "€", 2));
        assertEquals("1,234.56", Formatters.formatNumber(1234.56, 2, true));
        assertEquals("1234.56", Formatters.formatNumber(1234.56, 2, false));
    }

    @Test
    public void testHandlebarsExpressions() {
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> customer = new HashMap<>();
        customer.put("name", "John Doe");
        data.put("customer", customer);
        data.put("total", 2500.75);
        data.put("isActive", true);

        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> it1 = new HashMap<>();
        it1.put("desc", "Item 1");
        it1.put("price", 10);
        items.add(it1);

        Map<String, Object> it2 = new HashMap<>();
        it2.put("desc", "Item 2");
        it2.put("price", 20);
        items.add(it2);

        data.put("items", items);

        // Path resolution
        assertEquals("Hello John Doe", ExpressionEngine.evaluateTemplate("Hello {{customer.name}}", data));

        // Format helpers
        assertEquals("Total: $2,500.75", ExpressionEngine.evaluateTemplate("Total: {{currency total}}", data));
        assertTrue(ExpressionEngine.evaluateTemplate("Amount: {{words 1234}}", data).contains("One Thousand Two Hundred Thirty-Four"));

        // Upper helper
        assertEquals("JOHN DOE", ExpressionEngine.evaluateTemplate("{{upper customer.name}}", data));

        // If block
        assertEquals("Active!", ExpressionEngine.evaluateTemplate("{{#if isActive}}Active!{{/if}}", data));

        // Each block
        assertEquals("Item 1:10 Item 2:20 ", ExpressionEngine.evaluateTemplate("{{#each items}}{{this.desc}}:{{this.price}} {{/each}}", data));
    }

    @Test
    public void testArithmeticEvaluation() {
        Map<String, Object> data = new HashMap<>();
        data.put("price", 100);
        data.put("qty", 5);
        data.put("discount", 20);

        Object res = ExpressionEngine.evaluateExpression("(price * qty) - discount", data);
        assertEquals(480.0, ((Number) res).doubleValue(), 0.001);
    }
}
