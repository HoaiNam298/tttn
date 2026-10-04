package com.project.shopapp.services.impl;

import com.project.shopapp.dtos.CreateOrderRequest;
import com.project.shopapp.dtos.OrderItemRequest;
import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.model.Order;
import com.project.shopapp.model.OrderStatus;
import com.project.shopapp.model.Product;
import com.project.shopapp.model.User;
import com.project.shopapp.repositories.OrderRepository;
import com.project.shopapp.repositories.ProductRepository;
import com.project.shopapp.repositories.UserRepository;
import com.project.shopapp.responses.OrderResponse;
import com.project.shopapp.responses.OrderSummaryResponse;
import com.project.shopapp.services.OrderService;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {
    private static final BigDecimal SHIPPING_FEE = new BigDecimal("30000.00");

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public OrderResponse create(String phoneNumber, CreateOrderRequest request) {
        User user =
                userRepository
                        .findByPhoneNumber(phoneNumber)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Map<Long, Integer> quantities = aggregateItems(request.items());
        List<Product> products = productRepository.findAllByIdInOrderById(quantities.keySet());
        if (products.size() != quantities.size()) {
            throw new ResourceNotFoundException("One or more products no longer exist");
        }

        Order order =
                new Order(
                        user,
                        request.recipientName().trim(),
                        request.phoneNumber().trim(),
                        request.shippingAddress().trim(),
                        request.note() == null ? "" : request.note().trim(),
                        SHIPPING_FEE);
        for (Product product : products) {
            int quantity = quantities.get(product.getId());
            product.reserve(quantity);
            order.addItem(product, quantity);
        }
        return OrderResponse.from(orderRepository.save(order));
    }

    @Override
    public OrderResponse findOwnedOrder(Long id, String phoneNumber) {
        return orderRepository
                .findWithItemsByIdAndUserPhoneNumber(id, phoneNumber)
                .map(OrderResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    @Override
    public Page<OrderSummaryResponse> findOwnedOrders(String phoneNumber, Pageable pageable) {
        return orderRepository
                .findAllByUserPhoneNumber(phoneNumber, pageable)
                .map(OrderSummaryResponse::from);
    }

    @Override
    public Page<OrderSummaryResponse> findAll(OrderStatus status, Pageable pageable) {
        Page<Order> orders =
                status == null
                        ? orderRepository.findAll(pageable)
                        : orderRepository.findAllByStatus(status, pageable);
        return orders.map(OrderSummaryResponse::from);
    }

    @Override
    @Transactional
    public OrderResponse updateStatus(Long id, OrderStatus status) {
        Order order =
                orderRepository
                        .findWithItemsById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        order.updateStatus(status);
        if (status == OrderStatus.CANCELLED) {
            order.getItems().forEach(item -> item.getProduct().release(item.getQuantity()));
        }
        return OrderResponse.from(order);
    }

    private Map<Long, Integer> aggregateItems(List<OrderItemRequest> items) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (OrderItemRequest item : items) {
            quantities.merge(item.productId(), item.quantity(), Math::addExact);
        }
        return quantities;
    }
}
