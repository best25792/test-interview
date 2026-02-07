package com.example.orderservice.service;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderItemRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.UpdateOrderStatusRequest;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.entity.OrderStatus;
import com.example.orderservice.entity.Product;
import com.example.orderservice.exception.OrderNotFoundException;
import com.example.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;

    public List<OrderResponse> listOrders(OrderStatus status, String merchantId) {
        List<Order> orders;
        if (status != null && merchantId != null) {
            orders = orderRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
            orders = orders.stream().filter(o -> o.getStatus() == status).collect(Collectors.toList());
        } else if (status != null) {
            orders = orderRepository.findByStatusOrderByCreatedAtDesc(status);
        } else if (merchantId != null) {
            orders = orderRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
        } else {
            orders = orderRepository.findAllByOrderByCreatedAtDesc();
        }
        return orders.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public OrderResponse getOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        return toResponse(order);
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();

        for (OrderItemRequest itemReq : request.getItems()) {
            Product product = productService.getProductEntity(itemReq.getProductId());
            int qty = itemReq.getQuantity() != null ? itemReq.getQuantity() : 1;
            if (product.getStock() < qty) {
                throw new com.example.orderservice.exception.InsufficientStockException(
                        product.getName(), qty, product.getStock());
            }
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(qty));
            total = total.add(lineTotal);

            OrderItem item = OrderItem.builder()
                    .productId(product.getId())
                    .productName(product.getName())
                    .quantity(qty)
                    .unitPrice(product.getPrice())
                    .build();
            items.add(item);
        }

        Order order = Order.builder()
                .merchantId(request.getMerchantId())
                .customerUserId(request.getCustomerUserId())
                .paymentId(request.getPaymentId())
                .total(total)
                .currency("USD")
                .status(OrderStatus.PENDING)
                .build();

        order = orderRepository.save(order);

        for (OrderItem item : items) {
            item.setOrder(order);
            order.getItems().add(item);
        }
        order = orderRepository.save(order);

        for (OrderItem item : items) {
            productService.decreaseStock(item.getProductId(), item.getQuantity());
        }

        return toResponse(order);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long id, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
        order.setStatus(request.getStatus());
        order = orderRepository.save(order);
        return toResponse(order);
    }

    private OrderResponse toResponse(Order order) {
        List<com.example.orderservice.dto.OrderItemResponse> itemResponses = order.getItems() != null
                ? order.getItems().stream()
                .map(i -> com.example.orderservice.dto.OrderItemResponse.builder()
                        .id(i.getId())
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .lineTotal(i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                        .build())
                .collect(Collectors.toList())
                : List.of();

        return OrderResponse.builder()
                .id(order.getId())
                .merchantId(order.getMerchantId())
                .customerUserId(order.getCustomerUserId())
                .paymentId(order.getPaymentId())
                .total(order.getTotal())
                .currency(order.getCurrency())
                .status(order.getStatus())
                .items(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
