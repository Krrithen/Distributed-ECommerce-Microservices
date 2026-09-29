package com.example.orders.service.impl;

import com.example.orders.entity.Order;
import com.example.orders.entity.OrderItem;
import com.example.orders.repository.OrderRepository;
import com.example.orders.service.OrdersService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor // Automatically creates a constructor for all 'private final' fields
public class OrdersServiceImpl implements OrdersService {

    // Dependencies are final and injected via constructor (Standard Big Tech Practice)
    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public Order addOrdersDetails(Order order) {
        log.info("Adding order details for user: {}", order.getUserId());

        Order current = orderRepository.findTop1ByUserIdAndOrderStatusOrderByDateOfOrderDesc(
                order.getUserId(), "Payment Pending")
                .orElseThrow(() -> new IllegalArgumentException("Order not found for user: " + order.getUserId()));

        current.setProductCount(current.getOrderItems() != null ? current.getOrderItems().size() : 0);
        current.setTotalPrice(order.getTotalPrice());
        current.setOrderStatus(order.getOrderStatus());
        current.setModeOfPayment(order.getModeOfPayment());
        current.setShippingAddress(order.getShippingAddress());

        log.info("Updated order details for user: {}", order.getUserId());
        return orderRepository.save(current);
    }

    @Override
    public List<Order> getAllOrdersDetails() {
        log.info("Retrieving all order details");
        return orderRepository.findAll();
    }

    @Override
    @Transactional
    public Order addOrderProducts(List<OrderItem> items, String userId) {
        log.info("Adding order products for user: {}, products count: {}", userId, items.size());

        // Securely calculate the exact total price using BigDecimal math
        BigDecimal calculatedTotal = items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
                .orderId(Order.generateOrderId())
                .userId(userId)
                .orderItems(items) // Uses the cleanly mapped Java Entities
                .orderStatus("Payment Pending")
                .dateOfOrder(LocalDateTime.now())
                .shippingAddress("")
                .totalPrice(calculatedTotal)
                .modeOfPayment("")
                .productCount(items.size())
                .build();

        log.info("Successfully added order products for user: {}", userId);
        return orderRepository.save(order);
    }

    @Override
    public Order getOrdersDetails(String userId) {
        log.info("Retrieving order details for user: {}", userId);
        return orderRepository.findTop1ByUserIdAndOrderStatusOrderByDateOfOrderDesc(
                userId, "Payment Pending").orElse(null);
    }

    @Override
    public Order getOrderById(String orderId) {
        log.info("Retrieving order by ID: {}", orderId);
        return orderRepository.findByOrderId(orderId).orElse(null);
    }

    @Override
    public List<Order> getOrderHistory(String userId) {
        log.info("Retrieving order history for user: {}", userId);
        // I removed the reverse() logic. The database query already sorts by Date Descending (newest first).
        // Reversing it made it ascending (oldest first), which is generally bad UX for order history.
        return orderRepository.findByUserIdOrderByDateOfOrderDesc(userId);
    }
}