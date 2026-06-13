package com.edu.pcmaster.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record DashboardStatsResponse(
    BigDecimal totalRevenue,
    BigDecimal totalProfit,
    long activeOrders,
    long lowStockItems,
    long pendingPurchaseOrders,
    List<ActivityResponse> recentActivities,
    
    // Thống kê thực tế mới
    BigDecimal revenue30Days,
    BigDecimal cost30Days,
    long ordersCount30Days,
    long processingOrdersCount,
    List<TopProductResponse> topProducts,
    List<CategoryRevenueResponse> revenueByCategory,
    List<PeriodRevenueResponse> monthlyRevenue,
    List<PeriodRevenueResponse> quarterlyRevenue,
    List<PeriodRevenueResponse> yearlyRevenue
) {
    public record ActivityResponse(
        String title,
        String timeAgo,
        String type // e.g. "ORDER", "PURCHASE_ORDER", "PRODUCT"
    ) {}

    public record TopProductResponse(
        Long productId,
        String productName,
        long quantitySold,
        BigDecimal totalRevenue
    ) {}

    public record CategoryRevenueResponse(
        String categoryName,
        BigDecimal revenue
    ) {}

    public record PeriodRevenueResponse(
        String label, // e.g., "Tháng 06/2026", "Quý 1/2026", "Năm 2026"
        BigDecimal revenue,
        BigDecimal cost
    ) {}
}
