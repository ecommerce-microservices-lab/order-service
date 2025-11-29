package com.selimhorri.app.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.transaction.Transactional;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.DistributionSummary;
import org.springframework.stereotype.Service;

import com.selimhorri.app.domain.Order;
import com.selimhorri.app.domain.enums.OrderStatus;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.exception.wrapper.CartNotFoundException;
import com.selimhorri.app.exception.wrapper.OrderNotFoundException;
import com.selimhorri.app.helper.OrderMappingHelper;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.repository.OrderRepository;
import com.selimhorri.app.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@Slf4j
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

        private final OrderRepository orderRepository;
        private final CartRepository cartRepository;
        private final MeterRegistry meterRegistry;

        private Counter ordersCreatedCounter;
        private DistributionSummary orderValueSummary;

        @PostConstruct
        public void initMetric(){
            this.ordersCreatedCounter = Counter
                    .builder("orders_created_total")
                    .description("Órdenes creadas exitosamente")
                    .tag("service", "order")
                    .register(meterRegistry);

            this.orderValueSummary = DistributionSummary
                    .builder("order_value_usd")
                    .description("Distribución del valor de las órdenes")
                    .baseUnit("usd")
                    .publishPercentileHistogram()
                    .register(meterRegistry);
        }

        @Override
        public List<OrderDto> findAll() {
                log.info("*** OrderDto List, service; fetch all active orders *");
                return this.orderRepository.findAllByIsActiveTrue() // Cambia esto
                                .stream()
                                .map(OrderMappingHelper::map)
                                .distinct()
                                .collect(Collectors.toUnmodifiableList());
        }

        @Override
        public OrderDto findById(final Integer orderId) {
                log.info("*** OrderDto, service; fetch active order by id *");
                return this.orderRepository.findByOrderIdAndIsActiveTrue(orderId) // Cambia esto
                                .map(OrderMappingHelper::map)
                                .orElseThrow(() -> new OrderNotFoundException(
                                                String.format("Order with id: %d not found", orderId)));
        }

        @Override
        public OrderDto save(final OrderDto orderDto) {
                log.info("*** OrderDto, service; save order *");
                orderDto.setOrderId(null);
                orderDto.setOrderStatus(null);
                // Service-level validation
                if (orderDto.getCartDto() == null || orderDto.getCartDto().getCartId() == null) {
                        log.error("Order must be associated with a cart");
                        throw new IllegalArgumentException("Order must be associated with a cart");
                }

                // Check if cart exists
                cartRepository.findById(orderDto.getCartDto().getCartId())
                                .orElseThrow(() -> {
                                        log.error("Cart not found with ID: {}", orderDto.getCartDto().getCartId());
                                        return new CartNotFoundException(
                                                        "Cart not found with ID: " + orderDto.getCartDto().getCartId());
                                });

                // Proceed with saving if validations pass
                Order order = this.orderRepository.save(OrderMappingHelper.mapForCreationOrder(orderDto));
                ordersCreatedCounter.increment();
                orderValueSummary.record(order.getOrderFee());
                return OrderMappingHelper.map(order);
        }

        @Override
        public OrderDto updateStatus(final int orderId) {
                log.info("*** OrderDto, service; update order status *");
                try {
                        Order existingOrder = this.orderRepository
                                        .findByOrderIdAndIsActiveTrue(orderId)
                                        .orElseThrow(() -> new OrderNotFoundException(
                                                        "Order not found with ID: " + orderId));

                        // Actualizar al siguiente estado según la secuencia (Java 8+ compatible)
                        OrderStatus newStatus;
                        switch (existingOrder.getStatus()) {
                                case CREATED:
                                        newStatus = OrderStatus.ORDERED;
                                        break;
                                case ORDERED:
                                        newStatus = OrderStatus.IN_PAYMENT;
                                        break;
                                case IN_PAYMENT:
                                        throw new IllegalStateException(
                                                        "Order with ID " + orderId
                                                                        + " is already PAID and cannot be updated further");
                                default:
                                        throw new IllegalStateException(
                                                        "Unknown order status: " + existingOrder.getStatus());
                        }

                        existingOrder.setStatus(newStatus);
                        Order updatedOrder = this.orderRepository.save(existingOrder);

                        log.info("Order status updated successfully from {} to {}",
                                        existingOrder.getStatus(), newStatus);

                        return OrderMappingHelper.map(updatedOrder);

                } catch (Exception e) {
                        log.error("Error during order status update: ", e);
                        throw e;
                }
        }

        @Override
        public OrderDto update(final Integer orderId, final OrderDto orderDto) {
                log.info("*** OrderDto, service; update order with orderId *");
                orderDto.setOrderStatus(null);
                // Get existing order to preserve cart association
                Order existingOrder = this.orderRepository.findByOrderIdAndIsActiveTrue(orderId)
                                .orElseThrow(() -> new OrderNotFoundException("Order not found with ID: " + orderId));
                orderDto.setOrderId(orderId);
                // Map the updates but preserve the cart from existing order
                orderDto.setOrderStatus(existingOrder.getStatus());
                Order updatedOrder = OrderMappingHelper.mapForUpdate(orderDto, existingOrder.getCart());
                updatedOrder.setOrderDate(existingOrder.getOrderDate());
                return OrderMappingHelper.map(this.orderRepository.save(updatedOrder));
        }

        @Override
        public void deleteById(final Integer orderId) {
                Order order = orderRepository.findByOrderIdAndIsActiveTrue(orderId)
                                .orElseThrow(() -> new OrderNotFoundException("Order not found with id: " + orderId));

                // Solo permitir borrar si el estado es CREADO o PEDIDO
                if (order.getStatus() == OrderStatus.IN_PAYMENT) {
                        throw new IllegalStateException(
                                        "Cannot delete order with ID " + orderId + " because it's already PAID");
                }

                order.setActive(false);
                orderRepository.save(order);
                log.info("Order with id {} has been deactivated", orderId);
        }
}
