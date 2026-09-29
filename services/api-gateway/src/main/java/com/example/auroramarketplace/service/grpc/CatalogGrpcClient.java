package com.example.auroramarketplace.service.grpc;

import com.example.catalog.grpc.*;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class CatalogGrpcClient {

    @GrpcClient("catalog-service")
    private CatalogServiceGrpc.CatalogServiceBlockingStub catalogServiceStub;

    @Value("${grpc.client.deadline-ms:5000}")
    private long deadlineMs;

    private CatalogServiceGrpc.CatalogServiceBlockingStub stub() {
        return catalogServiceStub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS);
    }

    public ProductResponse createProduct(String name, String description, double price, int quantity, String categoryId, java.util.Map<String, String> attributes) {
        CreateProductRequest request = CreateProductRequest.newBuilder()
                .setName(name)
                .setDescription(description)
                .setPrice(price)
                .setQuantity(quantity)
                .setCategoryId(categoryId)
                .putAllAttributes(attributes)
                .build();
        return stub().createProduct(request);
    }

    public ProductResponse getProduct(String productId) {
        GetProductRequest request = GetProductRequest.newBuilder()
                .setProductId(productId)
                .build();
        return stub().getProduct(request);
    }
}
