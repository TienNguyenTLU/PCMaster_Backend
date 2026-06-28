package com.edu.pcmaster.services.chat;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class xử lý parse và clean các thông tin giá tiền
 * từ câu hỏi tiếng Việt của người dùng.
 */
public final class PriceUtils {

    private PriceUtils() {}

    // ── Price Constraint ──────────────────────────────────────

    public static class PriceConstraint {
        public BigDecimal minPrice = null;
        public BigDecimal maxPrice = null;

        public boolean hasConstraint() {
            return minPrice != null || maxPrice != null;
        }
    }

    // ── Parse price từ message ────────────────────────────────

    public static PriceConstraint parsePriceConstraints(String message) {
        PriceConstraint constraint = new PriceConstraint();
        if (message == null || message.isBlank()) {
            return constraint;
        }
        String text = message.toLowerCase().trim();

        // 1. Range: "từ 5tr - 8tr", "5 đến 8 triệu"
        Pattern rangePattern = Pattern.compile(
            "(?:từ\\s+)?([0-9.,]+)\\s*(triệu|tr|k|ngàn|nghìn)?\\s*(?:-|đến|tới)\\s*([0-9.,]+)\\s*(triệu|tr|k|ngàn|nghìn)?"
        );
        Matcher rangeMatcher = rangePattern.matcher(text);
        if (rangeMatcher.find()) {
            String unit1 = rangeMatcher.group(2);
            String unit2 = rangeMatcher.group(4);
            if (unit1 == null) unit1 = unit2;

            BigDecimal val1 = parseNumberAndUnit(rangeMatcher.group(1), unit1);
            BigDecimal val2 = parseNumberAndUnit(rangeMatcher.group(3), unit2);
            if (val1 != null && val2 != null) {
                constraint.minPrice = val1;
                constraint.maxPrice = val2;
                return constraint;
            }
        }

        // 2. Max: "dưới 8 triệu", "tối đa 8tr"
        Pattern maxPattern = Pattern.compile(
            "(?:dưới|tối đa|nhỏ hơn|thấp hơn|<)\\s*([0-9.,]+)\\s*(triệu|tr|k|ngàn|nghìn|đ|vnd)?"
        );
        Matcher maxMatcher = maxPattern.matcher(text);
        if (maxMatcher.find()) {
            BigDecimal val = parseNumberAndUnit(maxMatcher.group(1), maxMatcher.group(2));
            if (val != null) {
                constraint.maxPrice = val;
                return constraint;
            }
        }

        // 3. Min: "trên 8 triệu", "từ 8tr"
        Pattern minPattern = Pattern.compile(
            "(?:trên|tối thiểu|lớn hơn|cao hơn|từ|>)\\s*([0-9.,]+)\\s*(triệu|tr|k|ngàn|nghìn|đ|vnd)?"
        );
        Matcher minMatcher = minPattern.matcher(text);
        if (minMatcher.find()) {
            BigDecimal val = parseNumberAndUnit(minMatcher.group(1), minMatcher.group(2));
            if (val != null) {
                constraint.minPrice = val;
                return constraint;
            }
        }

        // 4. Approximate: "tầm 8 triệu" → ±20%
        Pattern approxPattern = Pattern.compile(
            "(?:tầm|khoảng|quanh|xung quanh|ở mức|budget)\\s*([0-9.,]+)\\s*(triệu|tr|k|ngàn|nghìn|đ|vnd)?"
        );
        Matcher approxMatcher = approxPattern.matcher(text);
        if (approxMatcher.find()) {
            BigDecimal val = parseNumberAndUnit(approxMatcher.group(1), approxMatcher.group(2));
            if (val != null) {
                constraint.minPrice = val.multiply(BigDecimal.valueOf(0.8));
                constraint.maxPrice = val.multiply(BigDecimal.valueOf(1.2));
                return constraint;
            }
        }

        return constraint;
    }

    // ── Clean price keywords khỏi query ──────────────────────

    public static String cleanPriceKeywords(String message) {
        if (message == null || message.isBlank()) return message;
        String text = message;

        // 1. Remove range patterns
        text = text.replaceAll("(?i)(?:mức\\s+)?(?:giá\\s+)?(?:tầm|khoảng|từ|ở|quanh|xung quanh)?\\s*\\d+[0-9.,]*\\s*(?:triệu|tr|k|ngàn|nghìn|đ|vnd)?\\s*(?:-|đến|tới)\\s*\\d+[0-9.,]*\\s*(?:triệu|tr|k|ngàn|nghìn|đ|vnd)?", "");

        // 2. Remove max patterns
        text = text.replaceAll("(?i)(?:mức\\s+)?(?:giá\\s+)?(?:dưới|tối đa|nhỏ hơn|thấp hơn|<)\\s*\\d+[0-9.,]*\\s*(?:triệu|tr|k|ngàn|nghìn|đ|vnd)?", "");

        // 3. Remove min patterns
        text = text.replaceAll("(?i)(?:mức\\s+)?(?:giá\\s+)?(?:trên|tối thiểu|lớn hơn|cao hơn|>)\\s*\\d+[0-9.,]*\\s*(?:triệu|tr|k|ngàn|nghìn|đ|vnd)?", "");

        // 4. Remove approximate patterns
        text = text.replaceAll("(?i)(?:mức\\s+)?(?:giá\\s+)?(?:tầm|khoảng|quanh|xung quanh|ở)\\s*\\d+[0-9.,]*\\s*(?:triệu|tr|k|ngàn|nghìn|đ|vnd)?", "");

        // 5. Remove standalone price units
        text = text.replaceAll("(?i)\\b\\d+[0-9.,]*\\s*(?:triệu|tr|k|ngàn|nghìn|đ|vnd)\\b", "");

        // Clean up
        text = text.replaceAll("\\s+", " ").trim();
        text = text.replaceAll("(?i)\\b(?:tầm|khoảng|giá|mức giá|ở mức|từ|budget)\\b\\s*$", "");
        text = text.replaceAll("\\s+", " ").trim();

        return text;
    }

    // ── Internal helpers ──────────────────────────────────────

    private static BigDecimal parseNumberAndUnit(String numStr, String unit) {
        if (numStr == null || numStr.isBlank()) return null;
        try {
            String cleanNum = numStr.replace(",", ".");
            if (cleanNum.contains(".")) {
                int firstDot = cleanNum.indexOf(".");
                int lastDot = cleanNum.lastIndexOf(".");
                if (firstDot != lastDot) {
                    cleanNum = cleanNum.replace(".", "");
                } else {
                    String suffix = cleanNum.substring(firstDot + 1);
                    if (suffix.length() == 3 && unit == null) {
                        cleanNum = cleanNum.replace(".", "");
                    }
                }
            }

            BigDecimal val = new BigDecimal(cleanNum);
            if (unit != null) {
                unit = unit.trim().toLowerCase();
                if (unit.equals("triệu") || unit.equals("tr")) {
                    val = val.multiply(new BigDecimal(1_000_000));
                } else if (unit.equals("k") || unit.equals("ngàn") || unit.equals("nghìn")) {
                    val = val.multiply(new BigDecimal(1_000));
                }
            } else {
                // Nếu không có đơn vị và số < 1000 → giả định đơn vị triệu
                if (val.compareTo(new BigDecimal(1000)) < 0) {
                    val = val.multiply(new BigDecimal(1_000_000));
                }
            }
            return val;
        } catch (Exception e) {
            return null;
        }
    }
}
