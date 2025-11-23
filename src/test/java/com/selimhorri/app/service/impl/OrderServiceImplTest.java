package com.selimhorri.app.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.domain.Order;
import com.selimhorri.app.domain.enums.OrderStatus;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.OrderDto;
import com.selimhorri.app.exception.wrapper.CartNotFoundException;
import com.selimhorri.app.exception.wrapper.OrderNotFoundException;
import com.selimhorri.app.repository.CartRepository;
import com.selimhorri.app.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl Tests")
class OrderServiceImplTest {

	@Mock
	private OrderRepository orderRepository;

	@Mock
	private CartRepository cartRepository;

	@InjectMocks
	private OrderServiceImpl orderService;

	private Order order;
	private OrderDto orderDto;
	private Cart cart;
	private CartDto cartDto;

	@BeforeEach
	void setUp() {
		cart = Cart.builder()
				.cartId(1)
				.userId(1)
				.isActive(true)
				.build();

		cartDto = CartDto.builder()
				.cartId(1)
				.userId(1)
				.build();

		order = Order.builder()
				.orderId(1)
				.orderDate(LocalDateTime.now())
				.orderDesc("Test Order")
				.orderFee(100.0)
				.status(OrderStatus.CREATED)
				.cart(cart)
				.isActive(true)
				.build();

		orderDto = OrderDto.builder()
				.orderId(1)
				.orderDate(LocalDateTime.now())
				.orderDesc("Test Order")
				.orderFee(100.0)
				.orderStatus(OrderStatus.CREATED)
				.cartDto(cartDto)
				.build();
	}

	@Test
	@DisplayName("Should find all active orders")
	void testFindAll() {
		// Given
		List<Order> orders = Arrays.asList(order);
		when(orderRepository.findAllByIsActiveTrue()).thenReturn(orders);

		// When
		List<OrderDto> result = orderService.findAll();

		// Then
		assertNotNull(result);
		assertFalse(result.isEmpty());
		verify(orderRepository, times(1)).findAllByIsActiveTrue();
	}

	@Test
	@DisplayName("Should find order by id when order exists")
	void testFindById_Success() {
		// Given
		Integer orderId = 1;
		when(orderRepository.findByOrderIdAndIsActiveTrue(orderId)).thenReturn(Optional.of(order));

		// When
		OrderDto result = orderService.findById(orderId);

		// Then
		assertNotNull(result);
		assertEquals(orderId, result.getOrderId());
		verify(orderRepository, times(1)).findByOrderIdAndIsActiveTrue(orderId);
	}

	@Test
	@DisplayName("Should throw OrderNotFoundException when order not found")
	void testFindById_NotFound() {
		// Given
		Integer orderId = 999;
		when(orderRepository.findByOrderIdAndIsActiveTrue(orderId)).thenReturn(Optional.empty());

		// When & Then
		assertThrows(OrderNotFoundException.class, () -> orderService.findById(orderId));
		verify(orderRepository, times(1)).findByOrderIdAndIsActiveTrue(orderId);
	}

	@Test
	@DisplayName("Should save order successfully")
	void testSave_Success() {
		// Given
		orderDto.setOrderId(null);
		orderDto.setOrderStatus(null);
		when(cartRepository.findById(cartDto.getCartId())).thenReturn(Optional.of(cart));
		when(orderRepository.save(any(Order.class))).thenReturn(order);

		// When
		OrderDto result = orderService.save(orderDto);

		// Then
		assertNotNull(result);
		verify(cartRepository, times(1)).findById(cartDto.getCartId());
		verify(orderRepository, times(1)).save(any(Order.class));
	}

	@Test
	@DisplayName("Should throw IllegalArgumentException when cart is null")
	void testSave_CartNull() {
		// Given
		orderDto.setCartDto(null);

		// When & Then
		assertThrows(IllegalArgumentException.class, () -> orderService.save(orderDto));
		verify(cartRepository, never()).findById(anyInt());
		verify(orderRepository, never()).save(any(Order.class));
	}

	@Test
	@DisplayName("Should throw CartNotFoundException when cart does not exist")
	void testSave_CartNotFound() {
		// Given
		when(cartRepository.findById(cartDto.getCartId())).thenReturn(Optional.empty());

		// When & Then
		assertThrows(CartNotFoundException.class, () -> orderService.save(orderDto));
		verify(cartRepository, times(1)).findById(cartDto.getCartId());
		verify(orderRepository, never()).save(any(Order.class));
	}

	@Test
	@DisplayName("Should update order status from CREATED to ORDERED")
	void testUpdateStatus_CreatedToOrdered() {
		// Given
		Integer orderId = 1;
		order.setStatus(OrderStatus.CREATED);
		when(orderRepository.findByOrderIdAndIsActiveTrue(orderId)).thenReturn(Optional.of(order));
		when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
			Order savedOrder = invocation.getArgument(0);
			savedOrder.setStatus(OrderStatus.ORDERED);
			return savedOrder;
		});

		// When
		OrderDto result = orderService.updateStatus(orderId);

		// Then
		assertNotNull(result);
		verify(orderRepository, times(1)).findByOrderIdAndIsActiveTrue(orderId);
		verify(orderRepository, times(1)).save(any(Order.class));
	}

	@Test
	@DisplayName("Should update order status from ORDERED to IN_PAYMENT")
	void testUpdateStatus_OrderedToInPayment() {
		// Given
		Integer orderId = 1;
		order.setStatus(OrderStatus.ORDERED);
		when(orderRepository.findByOrderIdAndIsActiveTrue(orderId)).thenReturn(Optional.of(order));
		when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
			Order savedOrder = invocation.getArgument(0);
			savedOrder.setStatus(OrderStatus.IN_PAYMENT);
			return savedOrder;
		});

		// When
		OrderDto result = orderService.updateStatus(orderId);

		// Then
		assertNotNull(result);
		verify(orderRepository, times(1)).findByOrderIdAndIsActiveTrue(orderId);
		verify(orderRepository, times(1)).save(any(Order.class));
	}

	@Test
	@DisplayName("Should throw IllegalStateException when order is already IN_PAYMENT")
	void testUpdateStatus_AlreadyInPayment() {
		// Given
		Integer orderId = 1;
		order.setStatus(OrderStatus.IN_PAYMENT);
		when(orderRepository.findByOrderIdAndIsActiveTrue(orderId)).thenReturn(Optional.of(order));

		// When & Then
		assertThrows(IllegalStateException.class, () -> orderService.updateStatus(orderId));
		verify(orderRepository, times(1)).findByOrderIdAndIsActiveTrue(orderId);
		verify(orderRepository, never()).save(any(Order.class));
	}

	@Test
	@DisplayName("Should throw OrderNotFoundException when order not found for update status")
	void testUpdateStatus_OrderNotFound() {
		// Given
		Integer orderId = 999;
		when(orderRepository.findByOrderIdAndIsActiveTrue(orderId)).thenReturn(Optional.empty());

		// When & Then
		assertThrows(OrderNotFoundException.class, () -> orderService.updateStatus(orderId));
		verify(orderRepository, times(1)).findByOrderIdAndIsActiveTrue(orderId);
		verify(orderRepository, never()).save(any(Order.class));
	}

	@Test
	@DisplayName("Should update order successfully")
	void testUpdate_Success() {
		// Given
		Integer orderId = 1;
		orderDto.setOrderDesc("Updated Order Description");
		orderDto.setOrderFee(200.0);
		when(orderRepository.findByOrderIdAndIsActiveTrue(orderId)).thenReturn(Optional.of(order));
		when(orderRepository.save(any(Order.class))).thenReturn(order);

		// When
		OrderDto result = orderService.update(orderId, orderDto);

		// Then
		assertNotNull(result);
		verify(orderRepository, times(1)).findByOrderIdAndIsActiveTrue(orderId);
		verify(orderRepository, times(1)).save(any(Order.class));
	}

	@Test
	@DisplayName("Should throw OrderNotFoundException when order not found for update")
	void testUpdate_OrderNotFound() {
		// Given
		Integer orderId = 999;
		when(orderRepository.findByOrderIdAndIsActiveTrue(orderId)).thenReturn(Optional.empty());

		// When & Then
		assertThrows(OrderNotFoundException.class, () -> orderService.update(orderId, orderDto));
		verify(orderRepository, times(1)).findByOrderIdAndIsActiveTrue(orderId);
		verify(orderRepository, never()).save(any(Order.class));
	}

	@Test
	@DisplayName("Should delete order successfully when status is CREATED")
	void testDeleteById_Success_Created() {
		// Given
		Integer orderId = 1;
		order.setStatus(OrderStatus.CREATED);
		when(orderRepository.findByOrderIdAndIsActiveTrue(orderId)).thenReturn(Optional.of(order));
		when(orderRepository.save(any(Order.class))).thenReturn(order);

		// When
		orderService.deleteById(orderId);

		// Then
		verify(orderRepository, times(1)).findByOrderIdAndIsActiveTrue(orderId);
		verify(orderRepository, times(1)).save(any(Order.class));
		assertFalse(order.isActive());
	}

	@Test
	@DisplayName("Should delete order successfully when status is ORDERED")
	void testDeleteById_Success_Ordered() {
		// Given
		Integer orderId = 1;
		order.setStatus(OrderStatus.ORDERED);
		when(orderRepository.findByOrderIdAndIsActiveTrue(orderId)).thenReturn(Optional.of(order));
		when(orderRepository.save(any(Order.class))).thenReturn(order);

		// When
		orderService.deleteById(orderId);

		// Then
		verify(orderRepository, times(1)).findByOrderIdAndIsActiveTrue(orderId);
		verify(orderRepository, times(1)).save(any(Order.class));
		assertFalse(order.isActive());
	}

	@Test
	@DisplayName("Should throw IllegalStateException when trying to delete order with IN_PAYMENT status")
	void testDeleteById_InPayment() {
		// Given
		Integer orderId = 1;
		order.setStatus(OrderStatus.IN_PAYMENT);
		when(orderRepository.findByOrderIdAndIsActiveTrue(orderId)).thenReturn(Optional.of(order));

		// When & Then
		assertThrows(IllegalStateException.class, () -> orderService.deleteById(orderId));
		verify(orderRepository, times(1)).findByOrderIdAndIsActiveTrue(orderId);
		verify(orderRepository, never()).save(any(Order.class));
	}

	@Test
	@DisplayName("Should throw OrderNotFoundException when order not found for delete")
	void testDeleteById_OrderNotFound() {
		// Given
		Integer orderId = 999;
		when(orderRepository.findByOrderIdAndIsActiveTrue(orderId)).thenReturn(Optional.empty());

		// When & Then
		assertThrows(OrderNotFoundException.class, () -> orderService.deleteById(orderId));
		verify(orderRepository, times(1)).findByOrderIdAndIsActiveTrue(orderId);
		verify(orderRepository, never()).save(any(Order.class));
	}

}

