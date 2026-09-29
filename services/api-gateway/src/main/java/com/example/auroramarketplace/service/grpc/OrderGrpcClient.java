package com.example.auroramarketplace.service.grpc;

import com.example.orders.grpc.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class OrderGrpcClient {

    @GrpcClient("order-service")
    private OrderServiceGrpc.OrderServiceBlockingStub orderServiceStub;

    @Value("${grpc.client.deadline-ms:5000}")
    private long deadlineMs;

    private OrderServiceGrpc.OrderServiceBlockingStub stub() {
        return orderServiceStub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS);
    }

    public OrderResponse createOrder(String userId, List<OrderItemProto> items) {
        CreateOrderRequest request = CreateOrderRequest.newBuilder()
                .setUserId(userId)
                .addAllItems(items)
                .build();
        return stub().createOrder(request);
    }

    public OrderResponse getOrder(String orderId) {
        GetOrderRequest request = GetOrderRequest.newBuilder()
                .setOrderId(orderId)
                .build();
        return stub().getOrder(request);
    }
}
