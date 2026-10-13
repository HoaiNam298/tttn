package com.project.shopapp.dtos.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ResponseJsonContractTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @ParameterizedTest(name = "{0}")
    @MethodSource("responseContracts")
    void preservesJsonFieldNames(Class<?> responseType, Set<String> expectedFields)
            throws Exception {
        Object response = responseType.getDeclaredConstructor().newInstance();
        JsonNode json = mapper.valueToTree(response);
        Set<String> actualFields = new HashSet<>();
        json.fieldNames().forEachRemaining(actualFields::add);
        assertEquals(expectedFields, actualFields);
    }

    @Test
    void preservesVnpayProtocolFieldNamesAndValues() throws Exception {
        VnpayIpnResponse response = new VnpayIpnResponse("00", "Confirmed");
        String json = mapper.writeValueAsString(response);
        assertEquals(
                mapper.readTree("{\"RspCode\":\"00\",\"Message\":\"Confirmed\"}"),
                mapper.readTree(json));
        VnpayIpnResponse restored = mapper.readValue(json, VnpayIpnResponse.class);
        assertEquals(response, restored);
    }

    private static Stream<Arguments> responseContracts() {
        return Stream.of(
                Arguments.of(
                        AddressResponse.class,
                        Set.of(
                                "id",
                                "recipientName",
                                "phoneNumber",
                                "shippingAddress",
                                "defaultAddress")),
                Arguments.of(
                        AuthResponse.class,
                        Set.of("accessToken", "tokenType", "expiresIn", "user")),
                Arguments.of(CategoryResponse.class, Set.of("id", "name")),
                Arguments.of(
                        CheckoutQuoteResponse.class,
                        Set.of(
                                "subtotal",
                                "shippingMethod",
                                "shippingFee",
                                "voucherCode",
                                "discount",
                                "total")),
                Arguments.of(
                        DailyRevenueResponse.class, Set.of("date", "completedOrders", "revenue")),
                Arguments.of(
                        DashboardResponse.class,
                        Set.of(
                                "from",
                                "to",
                                "timezone",
                                "revenue",
                                "completedOrders",
                                "estimatedCompletionCount",
                                "productCount",
                                "categoryCount",
                                "orderCount",
                                "statusCounts",
                                "dailyRevenue",
                                "topProducts",
                                "recentOrders")),
                Arguments.of(FavoriteStateResponse.class, Set.of("favorite")),
                Arguments.of(ImageUploadResponse.class, Set.of("url")),
                Arguments.of(
                        NotificationResponse.class,
                        Set.of("id", "title", "body", "link", "read", "createdAt")),
                Arguments.of(
                        OrderItemResponse.class,
                        Set.of(
                                "productId",
                                "productName",
                                "unitPrice",
                                "quantity",
                                "lineTotal",
                                "variantId",
                                "variantName",
                                "sku")),
                Arguments.of(
                        OrderResponse.class,
                        Set.of(
                                "id",
                                "orderNumber",
                                "recipientName",
                                "phoneNumber",
                                "shippingAddress",
                                "note",
                                "status",
                                "paymentMethod",
                                "subtotal",
                                "shippingFee",
                                "total",
                                "createdAt",
                                "items",
                                "shippingMethod",
                                "voucherCode",
                                "discount",
                                "history",
                                "paymentStatus",
                                "paymentExpiresAt")),
                Arguments.of(
                        OrderReviewStatusResponse.class,
                        Set.of("productId", "variantId", "reviewed")),
                Arguments.of(
                        OrderStatusHistoryResponse.class,
                        Set.of("status", "actor", "occurredAt", "imported")),
                Arguments.of(
                        OrderSummaryResponse.class,
                        Set.of(
                                "id",
                                "orderNumber",
                                "recipientName",
                                "phoneNumber",
                                "status",
                                "total",
                                "createdAt",
                                "paymentMethod",
                                "paymentStatus")),
                Arguments.of(
                        PaymentLinkResponse.class, Set.of("orderId", "paymentUrl", "expiresAt")),
                Arguments.of(
                        ProductDetailResponse.class,
                        Set.of(
                                "id",
                                "name",
                                "price",
                                "thumbnail",
                                "description",
                                "stock",
                                "category",
                                "images",
                                "variants",
                                "version")),
                Arguments.of(
                        ProductResponse.class,
                        Set.of(
                                "id",
                                "name",
                                "price",
                                "thumbnail",
                                "description",
                                "category",
                                "createdAt",
                                "updatedAt")),
                Arguments.of(
                        ProductReviewOverviewResponse.class,
                        Set.of("averageRating", "totalReviews", "reviews")),
                Arguments.of(
                        ProductReviewResponse.class,
                        Set.of(
                                "id",
                                "reviewerName",
                                "rating",
                                "comment",
                                "createdAt",
                                "images",
                                "shopReply",
                                "repliedAt",
                                "version",
                                "productId",
                                "productName")),
                Arguments.of(
                        ProductVariantResponse.class,
                        Set.of(
                                "id",
                                "sku",
                                "name",
                                "color",
                                "size",
                                "capacity",
                                "price",
                                "stock",
                                "imageUrl",
                                "active")),
                Arguments.of(
                        TopProductResponse.class,
                        Set.of("productId", "productName", "quantity", "grossSales")),
                Arguments.of(
                        UserResponse.class,
                        Set.of("id", "fullName", "phoneNumber", "address", "dateOfBirth", "role")),
                Arguments.of(VnpayIpnResponse.class, Set.of("RspCode", "Message")),
                Arguments.of(
                        VoucherResponse.class,
                        Set.of(
                                "id",
                                "code",
                                "type",
                                "amount",
                                "minimumSubtotal",
                                "maximumDiscount",
                                "startsAt",
                                "endsAt",
                                "usageLimit",
                                "perUserLimit",
                                "usedCount",
                                "targetUserId",
                                "active",
                                "version")));
    }
}
