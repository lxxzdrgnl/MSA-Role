package com.restaurant.order.service;

import com.restaurant.order.config.AppProperties;
import com.restaurant.order.dto.*;
import com.restaurant.order.entity.Order;
import com.restaurant.order.entity.OrderItem;
import com.restaurant.order.repository.OrderItemRepository;
import com.restaurant.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RestTemplate restTemplate;
    private final AppProperties appProperties;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        RestTemplate restTemplate,
                        AppProperties appProperties) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.restTemplate = restTemplate;
        this.appProperties = appProperties;
    }

    // ─── Public API ───────────────────────────────────────────────

    public OrderResponse createOrder(Long userId, OrderCreateRequest request) {
        List<OrderItem> items = new ArrayList<>();
        int totalPrice = 0;

        for (OrderCreateRequest.OrderItemRequest itemReq : request.getItems()) {
            Map<String, Object> menu = fetchMenu(itemReq.getMenuId());

            if (isSoldOut(menu)) {
                throw new IllegalArgumentException("Menu item is sold out: " + menu.get("name"));
            }

            int price = ((Number) menu.get("price")).intValue();
            String menuName = (String) menu.get("name");

            OrderItem item = new OrderItem();
            item.setMenuId(itemReq.getMenuId());
            item.setMenuName(menuName);
            item.setPrice(price);
            item.setQuantity(itemReq.getQuantity());
            items.add(item);

            totalPrice += price * itemReq.getQuantity();
        }

        Order order = new Order(userId, "PENDING", totalPrice);
        order = orderRepository.save(order);

        for (OrderItem item : items) {
            item.setOrderId(order.getId());
            orderItemRepository.save(item);
        }

        // Re-fetch to get DB-generated timestamps
        Order savedOrder = orderRepository.findById(order.getId()).orElse(order);

        notifyGateway(savedOrder.getId(), userId, "PENDING");

        return toResponse(savedOrder, items);
    }

    public PageResponse<OrderResponse> getMyOrders(Long userId, int page, int size) {
        int offset = (page - 1) * size;
        List<Order> orders = orderRepository.findByUserId(userId, offset, size);
        long totalCount = orderRepository.countByUserId(userId);
        int totalPages = (int) Math.ceil((double) totalCount / size);

        List<OrderResponse> responses = orders.stream()
                .map(o -> toResponse(o, orderItemRepository.findByOrderId(o.getId())))
                .collect(Collectors.toList());

        return new PageResponse<>(responses, page, totalPages, totalCount);
    }

    public PageResponse<OrderResponse> getAllOrders(int page, int size, String status) {
        int offset = (page - 1) * size;
        List<Order> orders;
        long totalCount;

        if (status != null && !status.isBlank()) {
            orders = orderRepository.findAllByStatus(status, offset, size);
            totalCount = orderRepository.countByStatusTotal(status);
        } else {
            orders = orderRepository.findAll(offset, size);
            totalCount = orderRepository.countAll();
        }

        int totalPages = (int) Math.ceil((double) totalCount / size);
        List<OrderResponse> responses = orders.stream()
                .map(o -> toResponse(o, orderItemRepository.findByOrderId(o.getId())))
                .collect(Collectors.toList());

        return new PageResponse<>(responses, page, totalPages, totalCount);
    }

    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        List<OrderItem> items = orderItemRepository.findByOrderId(id);
        return toResponse(order, items);
    }

    public List<OrderResponse> getActiveOrders() {
        List<Order> orders = orderRepository.findActiveOrders();
        return orders.stream()
                .map(o -> toResponse(o, orderItemRepository.findByOrderId(o.getId())))
                .collect(Collectors.toList());
    }

    public OrderResponse cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (!order.getUserId().equals(userId)) {
            throw new SecurityException("주문 취소 권한이 없습니다.");
        }

        if (!"PENDING".equals(order.getStatus())) {
            throw new IllegalStateException("주문이 이미 처리 중이거나 완료되어 취소할 수 없습니다.");
        }

        orderRepository.updateStatus(orderId, "CANCELLED");
        order.setStatus("CANCELLED");
        order.setUpdatedAt(LocalDateTime.now());

        notifyGateway(orderId, userId, "CANCELLED");

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return toResponse(order, items);
    }

    public OrderResponse updateStatus(Long orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        orderRepository.updateStatus(orderId, status);
        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());

        notifyGateway(orderId, order.getUserId(), status);

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        return toResponse(order, items);
    }

    public List<RevenueResponse> getRevenueStats(String period, String from, String to) {
        if (from == null || from.isBlank()) {
            from = LocalDate.now().minusMonths(1).toString();
        }
        if (to == null || to.isBlank()) {
            to = LocalDate.now().toString();
        }

        return switch (period != null ? period.toLowerCase() : "daily") {
            case "weekly" -> orderItemRepository.getRevenueByWeek(from, to);
            case "monthly" -> orderItemRepository.getRevenueByMonth(from, to);
            default -> orderItemRepository.getRevenueByDay(from, to);
        };
    }

    public List<BestSellerResponse> getBestSellers(String period, int limit) {
        String from;
        String to = LocalDate.now().toString();

        from = switch (period != null ? period.toLowerCase() : "weekly") {
            case "daily" -> LocalDate.now().toString();
            case "monthly" -> LocalDate.now().minusDays(30).toString();
            default -> LocalDate.now().minusDays(7).toString();
        };

        return orderItemRepository.getBestSellers(limit, from, to);
    }

    // ─── Internal API (for AI Operations) ─────────────────────────

    public Map<String, Object> getOrderHistory(int days) {
        LocalDateTime after = LocalDateTime.now().minusDays(days);
        List<Order> orders = orderRepository.findByCreatedAtAfter(after);

        // Collect all order items for these orders
        Map<Long, List<OrderItem>> orderItemsMap = new HashMap<>();
        for (Order o : orders) {
            orderItemsMap.put(o.getId(), orderItemRepository.findByOrderId(o.getId()));
        }

        // Menu-level order count aggregation
        Map<Long, Map<String, Object>> menuStats = new LinkedHashMap<>();
        for (Order o : orders) {
            List<OrderItem> items = orderItemsMap.getOrDefault(o.getId(), List.of());
            for (OrderItem item : items) {
                menuStats.computeIfAbsent(item.getMenuId(), k -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("menuId", item.getMenuId());
                    m.put("menuName", item.getMenuName());
                    m.put("totalQuantity", 0);
                    m.put("totalRevenue", 0);
                    return m;
                });
                Map<String, Object> stat = menuStats.get(item.getMenuId());
                stat.put("totalQuantity", (int) stat.get("totalQuantity") + item.getQuantity());
                stat.put("totalRevenue", (int) stat.get("totalRevenue") + item.getPrice() * item.getQuantity());
            }
        }

        // Hourly pattern (0-23)
        Map<Integer, Integer> hourlyPattern = new TreeMap<>();
        for (int h = 0; h < 24; h++) hourlyPattern.put(h, 0);
        for (Order o : orders) {
            if (o.getCreatedAt() != null) {
                int hour = o.getCreatedAt().getHour();
                hourlyPattern.merge(hour, 1, Integer::sum);
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalOrders", orders.size());
        result.put("days", days);
        result.put("menuStats", new ArrayList<>(menuStats.values()));
        result.put("hourlyPattern", hourlyPattern);

        return result;
    }

    public ActiveCountResponse getActiveCount() {
        int pending = orderRepository.countByStatus("PENDING");
        int cooking = orderRepository.countByStatus("COOKING");
        return new ActiveCountResponse(pending + cooking, pending, cooking);
    }

    // ─── Helper methods ───────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchMenu(Long menuId) {
        String url = appProperties.getMenuServiceUrl() + "/api/menus/" + menuId;
        try {
            return restTemplate.getForObject(url, Map.class);
        } catch (Exception e) {
            log.error("Failed to fetch menu {}: {}", menuId, e.getMessage());
            throw new IllegalArgumentException("Failed to fetch menu info for menuId: " + menuId);
        }
    }

    private boolean isSoldOut(Map<String, Object> menu) {
        // Handle both camelCase and snake_case keys
        for (String key : List.of("isSoldOut", "is_sold_out", "soldOut")) {
            Object val = menu.get(key);
            if (val instanceof Boolean && (Boolean) val) return true;
            if (val instanceof Number && ((Number) val).intValue() != 0) return true;
        }
        return false;
    }

    private void notifyGateway(Long orderId, Long userId, String status) {
        String url = appProperties.getGatewayUrl() + "/internal/ws/notify";
        try {
            Map<String, Object> payload = Map.of(
                    "type", "ORDER_UPDATE",
                    "orderId", orderId,
                    "userId", userId,
                    "status", status
            );
            restTemplate.postForEntity(url, payload, Void.class);
        } catch (Exception e) {
            log.warn("Failed to notify gateway for order {}: {}", orderId, e.getMessage());
        }
    }

    private OrderResponse toResponse(Order order, List<OrderItem> items) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setUserId(order.getUserId());
        response.setStatus(order.getStatus());
        response.setTotalPrice(order.getTotalPrice());
        response.setCreatedAt(order.getCreatedAt());
        response.setUpdatedAt(order.getUpdatedAt());

        List<OrderItemResponse> itemResponses = items.stream().map(item -> {
            OrderItemResponse ir = new OrderItemResponse();
            ir.setId(item.getId());
            ir.setMenuId(item.getMenuId());
            ir.setMenuName(item.getMenuName());
            ir.setPrice(item.getPrice());
            ir.setQuantity(item.getQuantity());
            return ir;
        }).collect(Collectors.toList());

        response.setItems(itemResponses);
        return response;
    }
}
