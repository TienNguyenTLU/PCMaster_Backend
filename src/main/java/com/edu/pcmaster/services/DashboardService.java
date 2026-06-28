package com.edu.pcmaster.services;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.edu.pcmaster.dto.dashboard.DashboardStatsResponse;
import com.edu.pcmaster.dto.dashboard.DashboardStatsResponse.ActivityResponse;
import com.edu.pcmaster.dto.dashboard.DashboardStatsResponse.CategoryRevenueResponse;
import com.edu.pcmaster.dto.dashboard.DashboardStatsResponse.PeriodRevenueResponse;
import com.edu.pcmaster.dto.dashboard.DashboardStatsResponse.TopProductResponse;
import com.edu.pcmaster.models.Order;
import com.edu.pcmaster.models.OrderItem;
import com.edu.pcmaster.models.OrderStatus;
import com.edu.pcmaster.models.Product;
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
        List<Order> allOrders = orderRepository.findAll();
        List<Product> allProducts = productRepository.findAll();

        
        List<Order> successfulOrders = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.CONFIRMED
                        || o.getStatus() == OrderStatus.SHIPPED
                        || o.getStatus() == OrderStatus.DELIVERED)
                .toList();

        BigDecimal totalRevenue = calculateRevenue(successfulOrders);
        BigDecimal totalCost = calculateCost(successfulOrders);
        BigDecimal totalProfit = totalRevenue.subtract(totalCost);

        
        long activeOrders = allOrders.stream()
                .filter(o -> o.getStatus() == OrderStatus.DRAFT
                        || o.getStatus() == OrderStatus.CONFIRMED
                        || o.getStatus() == OrderStatus.SHIPPED)
                .count();

        long lowStockItems = allProducts.stream()
                .filter(p -> p.getStock() < 10)
                .count();

        long pendingPurchaseOrders = purchaseOrderRepository.findAll().stream()
                .filter(po -> po.getStatus() == PurchaseOrderStatus.DRAFT)
                .count();

        
        List<ActivityResponse> activities = new ArrayList<>();
        allOrders.stream()
                .sorted((o1, o2) -> o2.getCreatedAt().compareTo(o1.getCreatedAt()))
                .limit(5)
                .forEach(o -> activities.add(new ActivityResponse(
                        "Đơn hàng mới #" + o.getId() + " - " + (o.getUser() != null ? o.getUser().getUsername() : "Khách vãng lai"), 
                        "Vừa xong", 
                        "ORDER")));

        
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        List<Order> orders30Days = allOrders.stream()
                .filter(o -> o.getCreatedAt().isAfter(thirtyDaysAgo))
                .toList();

        List<Order> successfulOrders30Days = orders30Days.stream()
                .filter(o -> o.getStatus() == OrderStatus.CONFIRMED
                        || o.getStatus() == OrderStatus.SHIPPED
                        || o.getStatus() == OrderStatus.DELIVERED)
                .toList();

        BigDecimal revenue30Days = calculateRevenue(successfulOrders30Days);
        BigDecimal cost30Days = calculateCost(successfulOrders30Days);
        
        long ordersCount30Days = orders30Days.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .count();

        long processingOrdersCount = activeOrders;

        
        Map<Product, Long> productQuantities = successfulOrders30Days.stream()
                .flatMap(o -> o.getItems().stream())
                .filter(item -> item.getProduct() != null)
                .collect(Collectors.groupingBy(
                        OrderItem::getProduct,
                        Collectors.summingLong(OrderItem::getQuantity)
                ));

        Map<Product, BigDecimal> productRevenues = successfulOrders30Days.stream()
                .flatMap(o -> o.getItems().stream())
                .filter(item -> item.getProduct() != null)
                .collect(Collectors.groupingBy(
                        OrderItem::getProduct,
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                item -> {
                                    BigDecimal price = item.getSellingPrice() != null ? item.getSellingPrice() : BigDecimal.ZERO;
                                    return price.multiply(BigDecimal.valueOf(item.getQuantity()));
                                },
                                BigDecimal::add
                        )
                ));

        List<TopProductResponse> topProducts = productQuantities.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .limit(5)
                .map(entry -> {
                    Product p = entry.getKey();
                    BigDecimal rev = productRevenues.getOrDefault(p, BigDecimal.ZERO);
                    return new TopProductResponse(
                            p.getId(),
                            p.getName(),
                            entry.getValue(),
                            rev
                    );
                })
                .toList();

        
        Map<String, BigDecimal> categoryRevenues = successfulOrders.stream()
                .flatMap(o -> o.getItems().stream())
                .filter(item -> item.getProduct() != null)
                .collect(Collectors.groupingBy(
                        item -> item.getProduct().getCategory() != null ? item.getProduct().getCategory().getName() : "Khác",
                        Collectors.reducing(
                                BigDecimal.ZERO,
                                item -> {
                                    BigDecimal price = item.getSellingPrice() != null ? item.getSellingPrice() : BigDecimal.ZERO;
                                    return price.multiply(BigDecimal.valueOf(item.getQuantity()));
                                },
                                BigDecimal::add
                        )
                ));

        List<CategoryRevenueResponse> revenueByCategory = categoryRevenues.entrySet().stream()
                .map(entry -> new CategoryRevenueResponse(entry.getKey(), entry.getValue()))
                .toList();

        
        ZoneId zoneId = ZoneId.of("Asia/Ho_Chi_Minh");

        
        Map<String, List<Order>> ordersByMonth = successfulOrders.stream()
                .collect(Collectors.groupingBy(o -> {
                    ZonedDateTime zdt = o.getCreatedAt().atZone(zoneId);
                    return zdt.format(DateTimeFormatter.ofPattern("MM/yyyy"));
                }));

        List<PeriodRevenueResponse> monthlyRevenue = ordersByMonth.entrySet().stream()
                .sorted((e1, e2) -> {
                    String[] parts1 = e1.getKey().split("/");
                    String[] parts2 = e2.getKey().split("/");
                    int y1 = Integer.parseInt(parts1[1]);
                    int y2 = Integer.parseInt(parts2[1]);
                    if (y1 != y2) return java.lang.Integer.compare(y1, y2);
                    return java.lang.Integer.compare(Integer.parseInt(parts1[0]), Integer.parseInt(parts2[0]));
                })
                .map(entry -> new PeriodRevenueResponse(
                        "Tháng " + entry.getKey(),
                        calculateRevenue(entry.getValue()),
                        calculateCost(entry.getValue())
                ))
                .toList();

        
        Map<String, List<Order>> ordersByQuarter = successfulOrders.stream()
                .collect(Collectors.groupingBy(o -> {
                    ZonedDateTime zdt = o.getCreatedAt().atZone(zoneId);
                    int quarter = (zdt.getMonthValue() - 1) / 3 + 1;
                    return "Q" + quarter + "/" + zdt.getYear();
                }));

        List<PeriodRevenueResponse> quarterlyRevenue = ordersByQuarter.entrySet().stream()
                .sorted((e1, e2) -> {
                    String[] parts1 = e1.getKey().split("/");
                    String[] parts2 = e2.getKey().split("/");
                    int y1 = Integer.parseInt(parts1[1]);
                    int y2 = Integer.parseInt(parts2[1]);
                    if (y1 != y2) return java.lang.Integer.compare(y1, y2);
                    return e1.getKey().compareTo(e2.getKey());
                })
                .map(entry -> new PeriodRevenueResponse(
                        "Quý " + entry.getKey().replace("Q", ""),
                        calculateRevenue(entry.getValue()),
                        calculateCost(entry.getValue())
                ))
                .toList();

        
        Map<String, List<Order>> ordersByYear = successfulOrders.stream()
                .collect(Collectors.groupingBy(o -> {
                    ZonedDateTime zdt = o.getCreatedAt().atZone(zoneId);
                    return String.valueOf(zdt.getYear());
                }));

        List<PeriodRevenueResponse> yearlyRevenue = ordersByYear.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new PeriodRevenueResponse(
                        "Năm " + entry.getKey(),
                        calculateRevenue(entry.getValue()),
                        calculateCost(entry.getValue())
                ))
                .toList();

        return new DashboardStatsResponse(
                totalRevenue, 
                totalProfit,
                activeOrders, 
                lowStockItems, 
                pendingPurchaseOrders, 
                activities,
                revenue30Days,
                cost30Days,
                ordersCount30Days,
                processingOrdersCount,
                topProducts,
                revenueByCategory,
                monthlyRevenue,
                quarterlyRevenue,
                yearlyRevenue
        );
    }

    private BigDecimal calculateRevenue(List<Order> orders) {
        return orders.stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal calculateCost(List<Order> orders) {
        return orders.stream()
                .flatMap(o -> o.getItems().stream())
                .map(item -> {
                    BigDecimal cost = item.getCostPrice() != null ? item.getCostPrice() : BigDecimal.ZERO;
                    return cost.multiply(BigDecimal.valueOf(item.getQuantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
