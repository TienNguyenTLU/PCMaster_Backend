package com.edu.pcmaster.models;

import com.edu.pcmaster.services.EmbeddingIngestionService;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PostRemove;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;
import java.util.concurrent.CompletableFuture;

/**
 * JPA Entity Listener listening to Product modifications.
 * Automatically synchronizes Product data with PGVector Store using EmbeddingIngestionService.
 */
@Component
public class ProductListener {

    private static EmbeddingIngestionService embeddingIngestionService;

    @Autowired
    public void setEmbeddingIngestionService(@Lazy EmbeddingIngestionService service) {
        ProductListener.embeddingIngestionService = service;
    }

    @PostPersist
    @PostUpdate
    public void onPostPersistOrUpdate(Product product) {
        if (embeddingIngestionService != null) {
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        CompletableFuture.runAsync(() -> {
                            try {
                                embeddingIngestionService.indexProduct(product);
                            } catch (Exception e) {
                                System.err.println("[RAG] Auto-indexing failed for product " + product.getId() + ": " + e.getMessage());
                            }
                        });
                    }
                });
            } else {
                CompletableFuture.runAsync(() -> {
                    try {
                        embeddingIngestionService.indexProduct(product);
                    } catch (Exception e) {
                        System.err.println("[RAG] Auto-indexing failed for product " + product.getId() + ": " + e.getMessage());
                    }
                });
            }
        }
    }

    @PostRemove
    public void onPostRemove(Product product) {
        if (embeddingIngestionService != null) {
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        CompletableFuture.runAsync(() -> {
                            try {
                                embeddingIngestionService.deleteProduct(product);
                            } catch (Exception e) {
                                System.err.println("[RAG] Auto-delete index failed for product " + product.getId() + ": " + e.getMessage());
                            }
                        });
                    }
                });
            } else {
                CompletableFuture.runAsync(() -> {
                    try {
                        embeddingIngestionService.deleteProduct(product);
                    } catch (Exception e) {
                        System.err.println("[RAG] Auto-delete index failed for product " + product.getId() + ": " + e.getMessage());
                    }
                });
            }
        }
    }
}

