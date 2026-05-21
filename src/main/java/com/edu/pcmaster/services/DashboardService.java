package com.edu.pcmaster.services;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.edu.pcmaster.dto.dashboard.DashboardStatsResponse;
import com.edu.pcmaster.models.OrderStatus;
import com.edu.pcmaster.models.PurchaseOrderStatus;
import com.edu.pcmaster.repositories.OrderRepository;
import com.edu.pcmaster.repositories.ProductRepository;
import com.edu.pcmaster.repositories.PurchaseOrderRepository;

@Service
public class DashboardService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public DashboardService(OrderRepository orderRepository, 
                           ProductRepository productRepository, 
                           PurchaseOrderRepository purchaseOrderRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    public DashboardStatsResponse getStats() {
        BigDecimal totalProfit = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.CONFIRMED
                        || o.getStatus() == OrderStatus.SHIPPED
                        || o.getStatus() == OrderStatus.DELIVERED)
                .flatMap(o -> o.getItems().stream())
                .map(item -> {
                    BigDecimal sell = item.getSellingPrice() != null ? item.getSellingPrice() : BigDecimal.ZERO;
                    BigDecimal cost = item.getCostPrice() != null ? item.getCostPrice() : BigDecimal.ZERO;
                    return sell.subtract(cost).multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalRevenue = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.CONFIRMED
                        || o.getStatus() == OrderStatus.SHIPPED
                        || o.getStatus() == OrderStatus.DELIVERED)
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long activeOrders = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() == OrderStatus.DRAFT
                        || o.getStatus() == OrderStatus.CONFIRMED
                        || o.getStatus() == OrderStatus.SHIPPED)
                .count();

        long lowStockItems = productRepository.findAll().stream()
                .filter(p -> p.getStock() < 10)
                .count();

        long pendingPurchaseOrders = purchaseOrderRepository.findAll().stream()
                .filter(po -> po.getStatus() == PurchaseOrderStatus.DRAFT)
                .count();

        // Recent activity - just some samples for now
        List<DashboardStatsResponse.ActivityResponse> activities = new ArrayList<>();
        orderRepository.findAll().stream()
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .limit(5)
                .forEach(o -> activities.add(new DashboardStatsResponse.ActivityResponse(
                        "Đơn hàng mới #" + o.getId(), 
                        "Vừa xong", 
                        "ORDER")));

        return new DashboardStatsResponse(
                totalRevenue, 
                totalProfit,
                activeOrders, 
                lowStockItems, 
                pendingPurchaseOrders, 
                activities);
    }
}
