package com.selimhorri.app.helper;

import java.time.LocalDateTime;

import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.domain.Order;
import com.selimhorri.app.domain.enums.OrderStatus;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.OrderDto;

public interface OrderMappingHelper {

        public static OrderDto map(final Order order) {
                return OrderDto.builder()
                                .orderId(order.getOrderId())
                                .orderDate(order.getOrderDate())
                                .orderDesc(order.getOrderDesc())
                                .orderFee(order.getOrderFee())
                                .orderStatus(order.getStatus())
                                .cartDto(
                                                CartDto.builder()
                                                                .cartId(order.getCart().getCartId())
                                                                .build())
                                .build();
        }

        public static Order map(final OrderDto orderDto) {
                return Order.builder()
                                .orderId(orderDto.getOrderId())
                                .orderDate(LocalDateTime.now())
                                .orderDesc(orderDto.getOrderDesc())
                                .orderFee(orderDto.getOrderFee())
                                .status(
                                                orderDto.getOrderStatus() != null
                                                                ? orderDto.getOrderStatus()
                                                                : OrderStatus.CREATED)
                                .cart(
                                                Cart.builder()
                                                                .cartId(orderDto.getCartDto().getCartId())
                                                                .build())
                                .build();
        }

        public static Order mapForCreationOrder(final OrderDto orderDto) {
                return Order.builder()
                                .orderId(orderDto.getOrderId())
                                .orderDate(LocalDateTime.now())
                                .orderDesc(orderDto.getOrderDesc())
                                .orderFee(orderDto.getOrderFee())
                                .isActive(true)
                                .status(
                                                orderDto.getOrderStatus() != null
                                                                ? orderDto.getOrderStatus()
                                                                : OrderStatus.CREATED)
                                .cart(
                                                Cart.builder()
                                                                .cartId(orderDto.getCartDto().getCartId())
                                                                .build())
                                .build();
        }

        // New method for update operations that preserves cart association
        public static Order mapForUpdate(final OrderDto orderDto, final Cart cart) {
                return Order.builder()
                                .orderId(orderDto.getOrderId())
                                .orderDate(orderDto.getOrderDate())
                                .orderDesc(orderDto.getOrderDesc())
                                .orderFee(orderDto.getOrderFee())
                                .cart(cart) // Preserve the existing cart
                                .build();
        }
}