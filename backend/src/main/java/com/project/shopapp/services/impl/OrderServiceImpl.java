package com.project.shopapp.services.impl;

import com.project.shopapp.dtos.CreateOrderRequest;
import com.project.shopapp.dtos.OrderItemRequest;
import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.model.Order;
import com.project.shopapp.model.OrderLineKey;
import com.project.shopapp.model.OrderStatus;
import com.project.shopapp.model.Product;
import com.project.shopapp.model.ProductVariant;
import com.project.shopapp.model.ShippingMethod;
import com.project.shopapp.model.User;
import com.project.shopapp.repositories.OrderRepository;
import com.project.shopapp.repositories.ProductRepository;
import com.project.shopapp.repositories.UserRepository;
import com.project.shopapp.responses.CheckoutQuoteResponse;
import com.project.shopapp.responses.OrderResponse;
import com.project.shopapp.responses.OrderSummaryResponse;
import com.project.shopapp.services.OrderService;
import com.project.shopapp.services.VoucherService;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final VoucherService vouchers;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            VoucherService vouchers) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.vouchers = vouchers;
    }

    @Override
    @Transactional
    public OrderResponse create(String phoneNumber, CreateOrderRequest request) {
        User user =
                (request.requestId() == null
                                ? userRepository.findByPhoneNumber(phoneNumber)
                                : userRepository.findForUpdateByPhoneNumber(phoneNumber))
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Map<OrderLineKey, Integer> quantities = aggregateItems(request.items());
        String fingerprint = request.requestId() == null ? null : fingerprint(request, quantities);
        if (request.requestId() != null) {
            var existing =
                    orderRepository.findByUserIdAndRequestId(user.getId(), request.requestId());
            if (existing.isPresent()) {
                if (!fingerprint.equals(existing.get().getRequestFingerprint())) {
                    throw new IllegalStateException(
                            "Request ID has already been used with different order data");
                }
                return OrderResponse.from(existing.get());
            }
        }
        var productIds =
                quantities.keySet().stream()
                        .map(OrderLineKey::productId)
                        .collect(java.util.stream.Collectors.toSet());
        List<Product> products = productRepository.findAllByIdInOrderById(productIds);
        if (products.size() != productIds.size()) {
            throw new ResourceNotFoundException("One or more products no longer exist");
        }

        Order order =
                new Order(
                        user,
                        request.recipientName().trim(),
                        request.phoneNumber().trim(),
                        request.shippingAddress().trim(),
                        request.note() == null ? "" : request.note().trim(),
                        shipping(request).getFee());
        order.selectShipping(shipping(request));
        if (request.requestId() != null) {
            order.identifyRequest(request.requestId(), fingerprint);
        }
        for (Product product : products) {
            for (var entry : quantities.entrySet()) {
                if (!entry.getKey().productId().equals(product.getId())) {
                    continue;
                }
                Long variantId = entry.getKey().variantId();
                ProductVariant variant =
                        variantId == null
                                ? null
                                : product.getVariants().stream()
                                        .filter(item -> variantId.equals(item.getId()))
                                        .findFirst()
                                        .orElseThrow(
                                                () ->
                                                        new IllegalArgumentException(
                                                                "Variant does not belong to the selected product"));
                int quantity = entry.getValue();
                product.reserve(quantity, variant);
                order.addItem(product, variant, quantity);
            }
        }
        if (hasVoucher(request)) {
            order.applyVoucher(
                    vouchers.reserve(request.voucherCode(), user.getId(), order.getSubtotal()));
        }
        if (request.expectedTotal() != null
                && request.expectedTotal().compareTo(order.getTotal()) != 0) {
            throw new IllegalStateException(
                    "Order total changed. Recalculate and confirm the new total");
        }
        return OrderResponse.from(orderRepository.save(order));
    }

    @Override
    public CheckoutQuoteResponse quote(String phoneNumber, CreateOrderRequest request) {
        User user =
                userRepository
                        .findByPhoneNumber(phoneNumber)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Map<OrderLineKey, Integer> quantities = aggregateItems(request.items());
        BigDecimal subtotal = BigDecimal.ZERO;
        var productIds =
                quantities.keySet().stream().map(OrderLineKey::productId).distinct().toList();
        var products = productRepository.findAllById(productIds);
        if (products.size() != productIds.size()) {
            throw new ResourceNotFoundException("One or more products no longer exist");
        }
        for (var entry : quantities.entrySet()) {
            Product product =
                    products.stream()
                            .filter(item -> entry.getKey().productId().equals(item.getId()))
                            .findFirst()
                            .orElseThrow();
            Long variantId = entry.getKey().variantId();
            ProductVariant variant =
                    variantId == null
                            ? null
                            : product.getVariants().stream()
                                    .filter(item -> variantId.equals(item.getId()))
                                    .findFirst()
                                    .orElseThrow(
                                            () ->
                                                    new IllegalArgumentException(
                                                            "Variant does not belong to the selected product"));
            if ((variant == null && !product.getVariants().isEmpty())
                    || (variant != null && !variant.isActive())) {
                throw new IllegalStateException("Select an available product variant");
            }
            int stock = variant == null ? product.getStock() : variant.getStock();
            if (stock < entry.getValue()) {
                throw new com.project.shopapp.exceptions.InsufficientStockException(
                        product.getName(), stock, entry.getValue());
            }
            BigDecimal price = variant == null ? product.getPrice() : variant.getPrice();
            subtotal = subtotal.add(price.multiply(BigDecimal.valueOf(entry.getValue())));
        }
        BigDecimal discount =
                hasVoucher(request)
                        ? vouchers.preview(request.voucherCode(), user.getId(), subtotal)
                        : BigDecimal.ZERO;
        String code =
                hasVoucher(request)
                        ? request.voucherCode().trim().toUpperCase(java.util.Locale.ROOT)
                        : null;
        return new CheckoutQuoteResponse(
                subtotal,
                shipping(request),
                shipping(request).getFee(),
                code,
                discount,
                subtotal.add(shipping(request).getFee()).subtract(discount));
    }

    private ShippingMethod shipping(CreateOrderRequest request) {
        return request.shippingMethod() == null
                ? ShippingMethod.STANDARD
                : request.shippingMethod();
    }

    private boolean hasVoucher(CreateOrderRequest request) {
        return request.voucherCode() != null && !request.voucherCode().isBlank();
    }

    @Override
    public OrderResponse findOwnedOrder(Long id, String phoneNumber) {
        return orderRepository
                .findWithItemsByIdAndUserPhoneNumber(id, phoneNumber)
                .map(OrderResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    @Override
    public OrderResponse findAdminOrder(Long id) {
        return orderRepository
                .findDetailById(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    @Override
    public Page<OrderSummaryResponse> findOwnedOrders(String phoneNumber, Pageable pageable) {
        return findOwnedOrders(phoneNumber, null, pageable);
    }

    @Override
    public Page<OrderSummaryResponse> findOwnedOrders(
            String phoneNumber, OrderStatus status, Pageable pageable) {
        Page<Order> orders =
                status == null
                        ? orderRepository.findAllByUserPhoneNumber(phoneNumber, pageable)
                        : orderRepository.findAllByUserPhoneNumberAndStatus(
                                phoneNumber, status, pageable);
        return orders.map(OrderSummaryResponse::from);
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
        if (status == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Only the order owner can confirm receipt");
        }
        Order order =
                orderRepository
                        .findWithItemsById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        order.updateStatus(status);
        if (status == OrderStatus.CANCELLED) {
            restoreStockAndVoucher(order);
        }
        return OrderResponse.from(order);
    }

    private void restoreStockAndVoucher(Order order) {
        var productIds =
                order.getItems().stream()
                        .map(item -> item.getProductId())
                        .collect(java.util.stream.Collectors.toSet());
        productRepository.findAllByIdInOrderById(productIds);
        order.getItems()
                .forEach(item -> item.getProduct().release(item.getQuantity(), item.getVariant()));
        if (order.getVoucher() != null) {
            vouchers.release(order.getVoucher().getId());
        }
    }

    @Override
    @Transactional
    public OrderResponse cancelOwnedOrder(Long id, String phoneNumber) {
        Order order =
                orderRepository
                        .findByIdAndUserPhoneNumber(id, phoneNumber)
                        .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (order.getStatus() == OrderStatus.CANCELLED) {
            return OrderResponse.from(order);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Only pending orders can be cancelled by the customer");
        }
        order.updateStatus(OrderStatus.CANCELLED, "CUSTOMER");
        restoreStockAndVoucher(order);
        return OrderResponse.from(order);
    }

    private Map<OrderLineKey, Integer> aggregateItems(List<OrderItemRequest> items) {
        Map<OrderLineKey, Integer> quantities = new TreeMap<>();
        for (OrderItemRequest item : items) {
            quantities.merge(
                    new OrderLineKey(item.productId(), item.variantId()),
                    item.quantity(),
                    Math::addExact);
        }
        if (quantities.values().stream().anyMatch(quantity -> quantity > 100 || quantity < 1)) {
            throw new IllegalArgumentException("Quantity per order line must be between 1 and 100");
        }
        return quantities;
    }

    private String fingerprint(CreateOrderRequest request, Map<OrderLineKey, Integer> quantities) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            hashField(digest, request.recipientName().trim());
            hashField(digest, request.phoneNumber().trim());
            hashField(digest, request.shippingAddress().trim());
            hashField(digest, request.note() == null ? "" : request.note().trim());
            new TreeMap<>(quantities)
                    .forEach(
                            (id, quantity) -> {
                                hashField(digest, id.productId().toString());
                                if (id.variantId() != null) {
                                    hashField(digest, "variant:" + id.variantId());
                                }
                                hashField(digest, quantity.toString());
                            });
            if (shipping(request) != ShippingMethod.STANDARD) {
                hashField(digest, "shipping:" + shipping(request));
            }
            if (hasVoucher(request)) {
                hashField(
                        digest,
                        "voucher:"
                                + request.voucherCode().trim().toUpperCase(java.util.Locale.ROOT));
            }
            if (request.expectedTotal() != null) {
                hashField(
                        digest,
                        "total:" + request.expectedTotal().stripTrailingZeros().toPlainString());
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private void hashField(MessageDigest digest, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
        digest.update(bytes);
    }

    @Override
    @Transactional
    public OrderResponse confirmReceipt(Long id, String phoneNumber) {
        Order order =
                orderRepository
                        .findByIdAndUserPhoneNumber(id, phoneNumber)
                        .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
        if (order.getStatus() != OrderStatus.COMPLETED) {
            if (order.getStatus() != OrderStatus.DELIVERED) {
                throw new IllegalStateException("Only delivered orders can be confirmed received");
            }
            order.updateStatus(OrderStatus.COMPLETED, "CUSTOMER");
        }
        return OrderResponse.from(order);
    }
}
