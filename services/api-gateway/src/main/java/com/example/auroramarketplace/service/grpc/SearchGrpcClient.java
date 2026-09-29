package com.example.auroramarketplace.service.grpc;

import com.example.search.grpc.SearchRequest;
import com.example.search.grpc.SearchResponse;
import com.example.search.grpc.SearchServiceGrpc;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class SearchGrpcClient {

    @GrpcClient("search-service")
    private SearchServiceGrpc.SearchServiceBlockingStub searchServiceStub;

    @Value("${grpc.client.deadline-ms:5000}")
    private long deadlineMs;

    public SearchResponse searchProducts(String query, int page, int size) {
        SearchRequest request = SearchRequest.newBuilder()
                .setQuery(query)
                .setPage(page)
                .setSize(size)
                .build();
        return searchServiceStub
                .withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS)
                .searchProducts(request);
    }
}
