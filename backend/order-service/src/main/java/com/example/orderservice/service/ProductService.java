package com.example.orderservice.service;

import com.example.orderservice.dto.CreateProductRequest;
import com.example.orderservice.dto.ProductResponse;
import com.example.orderservice.dto.UpdateStockRequest;
import com.example.orderservice.entity.Product;
import com.example.orderservice.exception.InsufficientStockException;
import com.example.orderservice.exception.ProductNotFoundException;
import com.example.orderservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public List<ProductResponse> listProducts() {
        return productRepository.findAllByOrderByIdAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public ProductResponse getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return toResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice() != null ? request.getPrice() : java.math.BigDecimal.ZERO)
                .currency(request.getCurrency() != null ? request.getCurrency() : "USD")
                .stock(request.getStock() != null ? request.getStock() : 0)
                .build();
        product = productRepository.save(product);
        return toResponse(product);
    }

    @Transactional
    public ProductResponse updateStock(Long id, UpdateStockRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        product.setStock(request.getStock());
        product = productRepository.save(product);
        return toResponse(product);
    }

    /**
     * Decrease stock for an order. Throws InsufficientStockException if not enough stock.
     */
    @Transactional
    public void decreaseStock(Long productId, int quantity) {
        Product product = productRepository.findByIdWithLock(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        int available = product.getStock();
        if (available < quantity) {
            throw new InsufficientStockException(product.getName(), quantity, available);
        }
        product.setStock(available - quantity);
        productRepository.save(product);
    }

    public Product getProductEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    private ProductResponse toResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .currency(p.getCurrency())
                .stock(p.getStock())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .build();
    }
}
