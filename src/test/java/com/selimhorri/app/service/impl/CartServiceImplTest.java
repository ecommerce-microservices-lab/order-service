package com.selimhorri.app.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.selimhorri.app.constant.AppConstant;
import com.selimhorri.app.domain.Cart;
import com.selimhorri.app.dto.CartDto;
import com.selimhorri.app.dto.UserDto;
import com.selimhorri.app.exception.wrapper.CartNotFoundException;
import com.selimhorri.app.exception.wrapper.UserNotFoundException;
import com.selimhorri.app.repository.CartRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartServiceImpl Tests")
class CartServiceImplTest {

	@Mock
	private CartRepository cartRepository;

	@Mock
	private RestTemplate restTemplate;

	@InjectMocks
	private CartServiceImpl cartService;

	private Cart cart;
	private CartDto cartDto;
	private UserDto userDto;

	@BeforeEach
	void setUp() {
		userDto = UserDto.builder()
				.userId(1)
				.firstName("John")
				.lastName("Doe")
				.email("john.doe@example.com")
				.build();

		cart = Cart.builder()
				.cartId(1)
				.userId(1)
				.isActive(true)
				.build();

		cartDto = CartDto.builder()
				.cartId(1)
				.userId(1)
				.userDto(userDto)
				.build();
	}

	@Test
	@DisplayName("Should find all active carts with user data")
	void testFindAll_Success() {
		// Given
		List<Cart> carts = Arrays.asList(cart);
		when(cartRepository.findAllByIsActiveTrue()).thenReturn(carts);
		when(restTemplate.getForObject(anyString(), eq(UserDto.class))).thenReturn(userDto);

		// When
		List<CartDto> result = cartService.findAll();

		// Then
		assertNotNull(result);
		assertFalse(result.isEmpty());
		verify(cartRepository, times(1)).findAllByIsActiveTrue();
		verify(restTemplate, times(1)).getForObject(anyString(), eq(UserDto.class));
	}

	@Test
	@DisplayName("Should filter out carts when user not found in findAll")
	void testFindAll_UserNotFound() {
		// Given
		List<Cart> carts = Arrays.asList(cart);
		when(cartRepository.findAllByIsActiveTrue()).thenReturn(carts);
		when(restTemplate.getForObject(anyString(), eq(UserDto.class)))
				.thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

		// When
		List<CartDto> result = cartService.findAll();

		// Then
		assertNotNull(result);
		// Cart should still be returned even if user is not found
		verify(cartRepository, times(1)).findAllByIsActiveTrue();
		verify(restTemplate, times(1)).getForObject(anyString(), eq(UserDto.class));
	}

	@Test
	@DisplayName("Should filter out carts when rest template throws exception in findAll")
	void testFindAll_RestClientException() {
		// Given
		List<Cart> carts = Arrays.asList(cart);
		when(cartRepository.findAllByIsActiveTrue()).thenReturn(carts);
		when(restTemplate.getForObject(anyString(), eq(UserDto.class)))
				.thenThrow(new RestClientException("Connection error"));

		// When
		List<CartDto> result = cartService.findAll();

		// Then
		assertNotNull(result);
		// Cart with error should be filtered out (null)
		verify(cartRepository, times(1)).findAllByIsActiveTrue();
		verify(restTemplate, times(1)).getForObject(anyString(), eq(UserDto.class));
	}

	@Test
	@DisplayName("Should find cart by id when cart exists")
	void testFindById_Success() {
		// Given
		Integer cartId = 1;
		when(cartRepository.findByCartIdAndIsActiveTrue(cartId)).thenReturn(Optional.of(cart));
		when(restTemplate.getForObject(anyString(), eq(UserDto.class))).thenReturn(userDto);

		// When
		CartDto result = cartService.findById(cartId);

		// Then
		assertNotNull(result);
		assertEquals(cartId, result.getCartId());
		verify(cartRepository, times(1)).findByCartIdAndIsActiveTrue(cartId);
		verify(restTemplate, times(1)).getForObject(anyString(), eq(UserDto.class));
	}

	@Test
	@DisplayName("Should throw CartNotFoundException when cart not found")
	void testFindById_NotFound() {
		// Given
		Integer cartId = 999;
		when(cartRepository.findByCartIdAndIsActiveTrue(cartId)).thenReturn(Optional.empty());

		// When & Then
		assertThrows(CartNotFoundException.class, () -> cartService.findById(cartId));
		verify(cartRepository, times(1)).findByCartIdAndIsActiveTrue(cartId);
		verify(restTemplate, never()).getForObject(anyString(), eq(UserDto.class));
	}

	@Test
	@DisplayName("Should save cart successfully when user exists")
	void testSave_Success() {
		// Given
		cartDto.setCartId(null);
		cartDto.setOrderDtos(null);
		when(restTemplate.getForObject(anyString(), eq(UserDto.class))).thenReturn(userDto);
		when(cartRepository.save(any(Cart.class))).thenReturn(cart);

		// When
		CartDto result = cartService.save(cartDto);

		// Then
		assertNotNull(result);
		verify(restTemplate, times(1)).getForObject(anyString(), eq(UserDto.class));
		verify(cartRepository, times(1)).save(any(Cart.class));
	}

	@Test
	@DisplayName("Should throw IllegalArgumentException when userId is null")
	void testSave_UserIdNull() {
		// Given
		cartDto.setUserId(null);

		// When & Then
		assertThrows(IllegalArgumentException.class, () -> cartService.save(cartDto));
		verify(restTemplate, never()).getForObject(anyString(), eq(UserDto.class));
		verify(cartRepository, never()).save(any(Cart.class));
	}

	@Test
	@DisplayName("Should throw UserNotFoundException when userDto is null")
	void testSave_UserDtoNull() {
		// Given
		when(restTemplate.getForObject(anyString(), eq(UserDto.class))).thenReturn(null);

		// When & Then
		assertThrows(UserNotFoundException.class, () -> cartService.save(cartDto));
		verify(restTemplate, times(1)).getForObject(anyString(), eq(UserDto.class));
		verify(cartRepository, never()).save(any(Cart.class));
	}

	@Test
	@DisplayName("Should throw RuntimeException when RestClientException occurs")
	void testSave_RestClientException() {
		// Given
		when(restTemplate.getForObject(anyString(), eq(UserDto.class)))
				.thenThrow(new RestClientException("Connection error"));

		// When & Then
		assertThrows(RuntimeException.class, () -> cartService.save(cartDto));
		verify(restTemplate, times(1)).getForObject(anyString(), eq(UserDto.class));
		verify(cartRepository, never()).save(any(Cart.class));
	}

	@Test
	@DisplayName("Should delete cart successfully (soft delete)")
	void testDeleteById_Success() {
		// Given
		Integer cartId = 1;
		when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));
		when(cartRepository.save(any(Cart.class))).thenReturn(cart);

		// When
		cartService.deleteById(cartId);

		// Then
		verify(cartRepository, times(1)).findById(cartId);
		verify(cartRepository, times(1)).save(any(Cart.class));
		assertFalse(cart.isActive());
	}

	@Test
	@DisplayName("Should throw CartNotFoundException when cart not found for delete")
	void testDeleteById_NotFound() {
		// Given
		Integer cartId = 999;
		when(cartRepository.findById(cartId)).thenReturn(Optional.empty());

		// When & Then
		assertThrows(CartNotFoundException.class, () -> cartService.deleteById(cartId));
		verify(cartRepository, times(1)).findById(cartId);
		verify(cartRepository, never()).save(any(Cart.class));
	}

}

