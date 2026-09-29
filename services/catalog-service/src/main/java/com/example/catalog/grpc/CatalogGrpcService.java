package com.example.catalog.grpc;

import com.example.catalog.grpc.CatalogServiceGrpc;
import com.example.catalog.grpc.*;
import com.example.catalog.entity.Product;
import com.example.catalog.service.CatalogService;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@GrpcService
@RequiredArgsConstructor
public class CatalogGrpcService extends CatalogServiceGrpc.CatalogServiceImplBase {

    private final CatalogService catalogService;

    @Override
    public void createProduct(CreateProductRequest request, StreamObserver<ProductResponse> responseObserver) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .quantity(request.getQuantity())
                .categoryId(request.getCategoryId())
                .attributes(request.getAttributesMap())
                .build();

        Product savedProduct = catalogService.createProduct(product);
        responseObserver.onNext(mapToResponse(savedProduct));
        responseObserver.onCompleted();
    }

    @Override
    public void getProduct(GetProductRequest request, StreamObserver<ProductResponse> responseObserver) {
        // Exceptions propagate to GrpcExceptionAdvice, which maps them to gRPC status codes
        Product product = catalogService.getProduct(request.getProductId());
        responseObserver.onNext(mapToResponse(product));
        responseObserver.onCompleted();
    }

    @Override
    public void listProducts(ListProductsRequest request, StreamObserver<ListProductsResponse> responseObserver) {
        List<Product> products = catalogService.listProducts(request.getCategoryId());
        List<ProductResponse> responses = products.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        ListProductsResponse response = ListProductsResponse.newBuilder()
                .addAllProducts(responses)
                .setTotalCount(responses.size())
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void batchGetProducts(BatchGetProductsRequest request, StreamObserver<BatchGetProductsResponse> responseObserver) {
        List<Product> products = catalogService.batchGetProducts(request.getProductIdsList());
        List<ProductResponse> responses = products.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        BatchGetProductsResponse response = BatchGetProductsResponse.newBuilder()
                .addAllProducts(responses)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.newBuilder()
                .setId(product.getId())
                .setName(product.getName())
                .setDescription(product.getDescription() != null ? product.getDescription() : "")
                .setPrice(product.getPrice() != null ? product.getPrice() : 0.0)
                .setQuantity(product.getQuantity() != null ? product.getQuantity() : 0)
                .setCategoryId(product.getCategoryId() != null ? product.getCategoryId() : "")
                .putAllAttributes(product.getAttributes() != null ? product.getAttributes() : java.util.Collections.emptyMap())
                .build();
    }
}
