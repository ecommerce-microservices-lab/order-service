package com.selimhorri.app.service;

import java.util.List;

import com.selimhorri.app.dto.CartDto;

public interface CartService {
	
	List<CartDto> findAll();
	CartDto findById(final Integer cartId);
	CartDto save(final CartDto cartDto);
	void deleteById(final Integer cartId);
	
}
