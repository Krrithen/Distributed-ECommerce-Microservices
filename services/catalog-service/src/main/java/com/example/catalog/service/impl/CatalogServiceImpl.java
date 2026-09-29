package com.example.catalog.service.impl;

import com.example.catalog.entity.Product;
import com.example.catalog.event.ProductCreatedEvent;
import com.example.catalog.exception.NotFoundException;
import com.example.catalog.repository.ProductRepository;
import com.example.catalog.service.CatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@Slf4j
@RequiredArgsConstructor
public class CatalogServiceImpl implements CatalogService {

    private final ProductRepository productRepository;
    private final KafkaTemplate<String, ProductCreatedEvent> kafkaTemplate;

    private static final String TOPIC_PRODUCT_EVENTS = "product-events";

    @Override
    public Product createProduct(Product product) {
        log.info("Creating product: {}", product.getName());
        
        // 1. Save to MongoDB
        Product savedProduct = productRepository.save(product);
        
        // 2. Publish event to Kafka (CQRS pattern)
        ProductCreatedEvent event = ProductCreatedEvent.builder()
                .id(savedProduct.getId())
                .name(savedProduct.getName())
                .description(savedProduct.getDescription())
                .price(savedProduct.getPrice())
                .categoryId(savedProduct.getCategoryId())
                .attributes(savedProduct.getAttributes())
                .build();
        
        kafkaTemplate.send(TOPIC_PRODUCT_EVENTS, savedProduct.getId(), event);
        log.info("Published ProductCreatedEvent for product id: {}", savedProduct.getId());

        return savedProduct;
    }

    @Override
    public Product getProduct(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));
    }

    @Override
    public List<Product> listProducts(String categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    @Override
    public List<Product> batchGetProducts(List<String> ids) {
        return StreamSupport.stream(productRepository.findAllById(ids).spliterator(), false)
                .collect(Collectors.toList());
    }
}
