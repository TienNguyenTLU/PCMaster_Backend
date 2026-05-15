package com.edu.pcmaster.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record DashboardStatsResponse(
    BigDecimal totalRevenue,
    long activeOrders,
    long lowStockItems,
    long pendingPurchaseOrders,
    List<ActivityResponse> recentActivities
) {
    public record ActivityResponse(
        String title,
        String timeAgo,
        String type // e.g. "ORDER", "PURCHASE_ORDER", "PRODUCT"
    ) {}
}
