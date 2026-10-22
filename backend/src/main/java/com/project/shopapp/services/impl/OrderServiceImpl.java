package com.project.shopapp.services.impl;

import com.project.shopapp.components.MessageKeys;
import com.project.shopapp.components.TransactionLog;
import com.project.shopapp.components.VnpayGateway;
import com.project.shopapp.configuration.VnpayProperties;
import com.project.shopapp.dtos.request.CreateOrderRequest;
import com.project.shopapp.dtos.request.OrderItemRequest;
import com.project.shopapp.dtos.response.CheckoutQuoteResponse;
import com.project.shopapp.dtos.response.OrderResponse;
import com.project.shopapp.dtos.response.OrderSummaryResponse;
import com.project.shopapp.enums.OrderStatus;
import com.project.shopapp.enums.PaymentMethod;
import com.project.shopapp.enums.PaymentStatus;
import com.project.shopapp.enums.ShippingMethod;
import com.project.shopapp.exceptions.InsufficientStockException;
import com.project.shopapp.exceptions.LocalizedArgumentException;
import com.project.shopapp.exceptions.LocalizedStateException;
import com.project.shopapp.exceptions.ResourceNotFoundException;
import com.project.shopapp.model.Order;
import com.project.shopapp.model.OrderItem;
import com.project.shopapp.model.Product;
import com.project.shopapp.model.ProductVariant;
import com.project.shopapp.model.User;
import com.project.shopapp.repositories.OrderItemRepository;
import com.project.shopapp.repositories.OrderRepository;
import com.project.shopapp.repositories.OrderStatusHistoryRepository;
import com.project.shopapp.repositories.ProductRepository;
import com.project.shopapp.repositories.UserRepository;
import com.project.shopapp.services.OrderBusinessService;
import com.project.shopapp.services.OrderService;
import com.project.shopapp.services.VoucherService;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final VoucherService vouchers;
    private final VnpayProperties vnpay;
    private final OrderItemRepository orderItems;
    private final OrderStatusHistoryRepository orderHistory;
    private final OrderBusinessService orderBusinessService;

    public OrderServiceImpl(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            VoucherService vouchers,
            VnpayProperties vnpay,
            OrderItemRepository orderItems,
            OrderStatusHistoryRepository orderHistory,
            OrderBusinessService orderBusinessService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.vouchers = vouchers;
        this.vnpay = vnpay;
        this.orderItems = orderItems;
        this.orderHistory = orderHistory;
        this.orderBusinessService = orderBusinessService;
    }

    @Override
    @Transactional
    public OrderResponse create(String phoneNumber, CreateOrderRequest request) {
        if (request.getPaymentMethod() == PaymentMethod.VNPAY) {
            if (request.getRequestId() == null) {
                throw new LocalizedArgumentException(MessageKeys.ORDER_REQUEST_ID_REQUIRED);
            }
            if (vnpay == null) {
                throw new LocalizedStateException(MessageKeys.PAYMENT_NOT_CONFIGURED);
            }
            vnpay.requireEnabled();
        }
        var matchedUser =
                request.getRequestId() == null
                        ? userRepository.findByPhoneNumber(phoneNumber)
                        : userRepository.findForUpdateByPhoneNumber(phoneNumber);
        User user =
                matchedUser.orElseThrow(
                        () -> new ResourceNotFoundException(MessageKeys.USER_NOT_FOUND));
        Map<OrderLineKey, Integer> quantities = aggregateItems(request.getItems());
        String fingerprint =
                request.getRequestId() == null ? null : fingerprint(request, quantities);
        if (request.getRequestId() != null) {
            var existing =
                    orderRepository.findByUserIdAndRequestId(user.getId(), request.getRequestId());
            if (existing.isPresent()) {
                if (!fingerprint.equals(existing.get().getRequestFingerprint())) {
                    log.warn(
                            "Order retry rejected orderId={} reason=different_order_data",
                            existing.get().getId());
                    throw new LocalizedStateException(MessageKeys.ORDER_REQUEST_CONFLICT);
                }
                log.debug("Order retry reused existing orderId={}", existing.get().getId());
                return OrderResponse.from(existing.get());
            }
        }
        var productIds =
                quantities.keySet().stream()
                        .map(OrderLineKey::productId)
                        .collect(Collectors.toSet());
        List<Product> products = productRepository.findAllByIdInOrderById(productIds);
        if (products.size() != productIds.size()) {
            throw new ResourceNotFoundException(MessageKeys.ORDER_PRODUCTS_MISSING);
        }

        Order order =
                orderBusinessService.createOrder(
                        user,
                        request.getRecipientName().trim(),
                        request.getPhoneNumber().trim(),
                        request.getShippingAddress().trim(),
                        request.getNote() == null ? "" : request.getNote().trim(),
                        shipping(request).getFee());
        orderBusinessService.selectShipping(order, shipping(request));
        if (request.getRequestId() != null) {
            order.setRequestId(request.getRequestId());
            order.setRequestFingerprint(fingerprint);
        }
        for (Product product : products) {
            for (var entry : quantities.entrySet()) {
                if (!entry.getKey().productId().equals(product.getId())) {
                    continue;
                }
                Long variantId = entry.getKey().variantId();
                ProductVariant variant = findVariant(product, variantId);
                int quantity = entry.getValue();
                product.reserve(quantity, variant);
                orderBusinessService.addItem(order, product, variant, quantity);
            }
        }
        if (hasVoucher(request)) {
            orderBusinessService.applyVoucher(
                    order,
                    vouchers.reserve(request.getVoucherCode(), user.getId(), order.getSubtotal()));
        }
        if (request.getExpectedTotal() != null
                && request.getExpectedTotal().compareTo(order.getTotal()) != 0) {
            throw new LocalizedStateException(MessageKeys.ORDER_TOTAL_CHANGED);
        }
        if (request.getPaymentMethod() == PaymentMethod.VNPAY) {
            VnpayGateway.amount(order.getTotal());
            orderBusinessService.selectOnlinePayment(order, Instant.now().plusSeconds(900));
        }
        orderRepository.save(order);
        orderItems.saveAll(order.getItems());
        orderHistory.saveAll(order.getHistory());
        TransactionLog.info(
                log,
                "Order created orderId={} userId={} paymentMethod={}",
                order.getId(),
                user.getId(),
                order.getPaymentMethod());
        return OrderResponse.from(order);
    }

    @Override
    public CheckoutQuoteResponse quote(String phoneNumber, CreateOrderRequest request) {
        User user =
                userRepository
                        .findByPhoneNumber(phoneNumber)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(MessageKeys.USER_NOT_FOUND));
        Map<OrderLineKey, Integer> quantities = aggregateItems(request.getItems());
        BigDecimal subtotal = BigDecimal.ZERO;
        var productIds =
                quantities.keySet().stream().map(OrderLineKey::productId).distinct().toList();
        var products = productRepository.findAllById(productIds);
        if (products.size() != productIds.size()) {
            throw new ResourceNotFoundException(MessageKeys.ORDER_PRODUCTS_MISSING);
        }
        for (var entry : quantities.entrySet()) {
            Product product =
                    products.stream()
                            .filter(item -> entry.getKey().productId().equals(item.getId()))
                            .findFirst()
                            .orElseThrow();
            Long variantId = entry.getKey().variantId();
            ProductVariant variant = findVariant(product, variantId);
            if ((variant == null && !product.getVariants().isEmpty())
                    || (variant != null && !variant.isActive())) {
                throw new LocalizedStateException(MessageKeys.PRODUCT_VARIANT_UNAVAILABLE);
            }
            int stock = variant == null ? product.getStock() : variant.getStock();
            if (stock < entry.getValue()) {
                throw new InsufficientStockException(product.getName(), stock, entry.getValue());
            }
            BigDecimal price = variant == null ? product.getPrice() : variant.getPrice();
            subtotal = subtotal.add(price.multiply(BigDecimal.valueOf(entry.getValue())));
        }
        BigDecimal discount =
                hasVoucher(request)
                        ? vouchers.preview(request.getVoucherCode(), user.getId(), subtotal)
                        : BigDecimal.ZERO;
        String code =
                hasVoucher(request)
                        ? request.getVoucherCode().trim().toUpperCase(Locale.ROOT)
                        : null;
        ShippingMethod shippingMethod = shipping(request);
        BigDecimal shippingFee = shippingMethod.getFee();
        return CheckoutQuoteResponse.builder()
                .subtotal(subtotal)
                .shippingMethod(shippingMethod)
                .shippingFee(shippingFee)
                .voucherCode(code)
                .discount(discount)
                .total(subtotal.add(shippingFee).subtract(discount))
                .build();
    }

    private ShippingMethod shipping(CreateOrderRequest request) {
        return request.getShippingMethod() == null
                ? ShippingMethod.STANDARD
                : request.getShippingMethod();
    }

    private boolean hasVoucher(CreateOrderRequest request) {
        return request.getVoucherCode() != null && !request.getVoucherCode().isBlank();
    }

    @Override
    public OrderResponse findOwnedOrder(Long id, String phoneNumber) {
        return orderRepository
                .findWithItemsByIdAndUserPhoneNumber(id, phoneNumber)
                .map(OrderResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.ORDER_NOT_FOUND, id));
    }

    @Override
    public OrderResponse findAdminOrder(Long id) {
        return orderRepository
                .findDetailById(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException(MessageKeys.ORDER_NOT_FOUND, id));
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
            throw new LocalizedStateException(MessageKeys.ORDER_OWNER_REQUIRED);
        }
        Order order =
                orderRepository
                        .findWithItemsById(id)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                MessageKeys.ORDER_NOT_FOUND, id));
        changeStatus(order, status);
        if (status == OrderStatus.CANCELLED) {
            restoreStockAndVoucher(order);
        }
        return OrderResponse.from(order);
    }

    private void restoreStockAndVoucher(Order order) {
        var productIds =
                order.getItems().stream().map(OrderItem::getProductId).collect(Collectors.toSet());
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
                        .orElseThrow(
                                () -> new ResourceNotFoundException(MessageKeys.ORDER_NOT_FOUND));
        if (order.getStatus() == OrderStatus.CANCELLED) {
            log.debug("Order cancellation skipped orderId={} reason=already_cancelled", id);
            return OrderResponse.from(order);
        }
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new LocalizedStateException(MessageKeys.ORDER_CANCELLATION_UNAVAILABLE);
        }
        changeStatus(order, OrderStatus.CANCELLED, "CUSTOMER");
        restoreStockAndVoucher(order);
        return OrderResponse.from(order);
    }

    private Map<OrderLineKey, Integer> aggregateItems(List<OrderItemRequest> items) {
        Map<OrderLineKey, Integer> quantities = new TreeMap<>();
        for (OrderItemRequest item : items) {
            quantities.merge(
                    new OrderLineKey(item.getProductId(), item.getVariantId()),
                    item.getQuantity(),
                    Math::addExact);
        }
        if (quantities.values().stream().anyMatch(quantity -> quantity > 100 || quantity < 1)) {
            throw new LocalizedArgumentException(MessageKeys.ORDER_INVALID_QUANTITY);
        }
        return quantities;
    }

    private String fingerprint(CreateOrderRequest request, Map<OrderLineKey, Integer> quantities) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            if (request.getPaymentMethod() == PaymentMethod.VNPAY) {
                hashField(digest, "payment:VNPAY");
            }
            hashField(digest, request.getRecipientName().trim());
            hashField(digest, request.getPhoneNumber().trim());
            hashField(digest, request.getShippingAddress().trim());
            hashField(digest, request.getNote() == null ? "" : request.getNote().trim());
            for (var entry : new TreeMap<>(quantities).entrySet()) {
                OrderLineKey id = entry.getKey();
                hashField(digest, id.productId().toString());
                if (id.variantId() != null) {
                    hashField(digest, "variant:" + id.variantId());
                }
                hashField(digest, entry.getValue().toString());
            }
            if (shipping(request) != ShippingMethod.STANDARD) {
                hashField(digest, "shipping:" + shipping(request));
            }
            if (hasVoucher(request)) {
                hashField(
                        digest,
                        "voucher:" + request.getVoucherCode().trim().toUpperCase(Locale.ROOT));
            }
            if (request.getExpectedTotal() != null) {
                hashField(
                        digest,
                        "total:" + request.getExpectedTotal().stripTrailingZeros().toPlainString());
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
    public void closeUnpaidPayment(Long id, PaymentStatus reason) {
        if (reason != PaymentStatus.FAILED && reason != PaymentStatus.EXPIRED) {
            throw new LocalizedArgumentException(MessageKeys.PAYMENT_INVALID_CLOSURE);
        }
        Order order =
                orderRepository
                        .findWithItemsById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException(MessageKeys.ORDER_NOT_FOUND));
        if (order.getPaymentMethod() != PaymentMethod.VNPAY
                || order.getPaymentStatus() != PaymentStatus.PENDING
                || order.getStatus() != OrderStatus.PENDING) {
            return;
        }
        if (reason == PaymentStatus.EXPIRED && order.getPaymentExpiresAt().isAfter(Instant.now())) {
            return;
        }
        orderBusinessService.recordPayment(order, reason);
        TransactionLog.info(log, "Unpaid payment closed orderId={} reason={}", id, reason);
        changeStatus(order, OrderStatus.CANCELLED, "SYSTEM");
        restoreStockAndVoucher(order);
    }

    @Override
    @Transactional
    public OrderResponse confirmReceipt(Long id, String phoneNumber) {
        Order order =
                orderRepository
                        .findByIdAndUserPhoneNumber(id, phoneNumber)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                MessageKeys.ORDER_NOT_FOUND, id));
        if (order.getStatus() != OrderStatus.COMPLETED) {
            if (order.getStatus() != OrderStatus.DELIVERED) {
                throw new LocalizedStateException(MessageKeys.ORDER_RECEIPT_UNAVAILABLE);
            }
            changeStatus(order, OrderStatus.COMPLETED, "CUSTOMER");
        }
        return OrderResponse.from(order);
    }

    private void changeStatus(Order order, OrderStatus status, String actor) {
        OrderStatus previous = order.getStatus();
        orderHistory.save(orderBusinessService.updateStatus(order, status, actor));
        TransactionLog.info(
                log,
                "Order status changed orderId={} previousStatus={} status={} actor={}",
                order.getId(),
                previous,
                status,
                actor);
    }

    private void changeStatus(Order order, OrderStatus status) {
        changeStatus(order, status, "ADMIN");
    }

    private ProductVariant findVariant(Product product, Long variantId) {
        if (variantId == null) {
            return null;
        }
        ProductVariant variant =
                product.getVariants().stream()
                        .filter(item -> variantId.equals(item.getId()))
                        .findFirst()
                        .orElse(null);
        if (variant == null) {
            throw new LocalizedArgumentException(MessageKeys.PRODUCT_VARIANT_MISMATCH);
        }
        return variant;
    }

    private record OrderLineKey(Long productId, Long variantId)
            implements Comparable<OrderLineKey> {
        @Override
        public int compareTo(OrderLineKey other) {
            int result = productId.compareTo(other.productId);
            if (result != 0) {
                return result;
            }
            if (variantId == null) {
                return other.variantId == null ? 0 : -1;
            }
            return other.variantId == null ? 1 : variantId.compareTo(other.variantId);
        }
    }
}
