package com.selimhorri.app.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.selimhorri.app.domain.Cart;

public interface CartRepository extends JpaRepository<Cart, Integer> {

    List<Cart> findAllByIsActiveTrue();

    Optional<Cart> findByCartIdAndIsActiveTrue(Integer cartId);

}