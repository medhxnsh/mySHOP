package com.myshop.service;

import com.myshop.dto.request.OrderRequest;
import com.myshop.exception.BusinessException;
import com.myshop.model.entity.Cart;
import com.myshop.model.entity.CartItem;
import com.myshop.model.entity.Product;
import com.myshop.model.entity.User;
import com.myshop.repository.jpa.CartRepository;
import com.myshop.repository.jpa.OrderRepository;
import com.myshop.repository.jpa.ProductRepository;
import com.myshop.repository.jpa.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.myshop.mapper.OrderMapper orderMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    private User testUser;
    private Product testProduct;
    private Cart testCart;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .build();

        testProduct = Product.builder()
                .id(UUID.randomUUID())
                .name("Test Product")
                .price(new BigDecimal("100.00"))
                .stockQuantity(10)
                .build();

        testCart = Cart.builder()
                .id(UUID.randomUUID())
                .user(testUser)
                .build();
    }

    @Test
    void placeOrder_InsufficientStock_ThrowsException() {
        // Arrange
        // Product has 10 in stock, cart wants 15.
        testProduct.setStockQuantity(10);

        CartItem cartItem = CartItem.builder()
                .cart(testCart)
                .product(testProduct)
                .quantity(15) // Greater than available stock!
                .build();
        testCart.addItem(cartItem);

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));

        OrderRequest request = new OrderRequest(Map.of("street", "123 Main St"), "COD");

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () -> {
            orderService.placeOrder(testUser.getEmail(), request);
        });

        assertEquals("INSUFFICIENT_STOCK", ex.getErrorCode().name());
        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void placeOrder_EmptyCart_ThrowsException() {
        // Arrange
        testCart.getItems().clear(); // Empty cart

        when(userRepository.findByEmail(testUser.getEmail())).thenReturn(Optional.of(testUser));
        when(cartRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testCart));

        OrderRequest request = new OrderRequest(Collections.singletonMap("street", "123 Main St"), "COD");

        // Act & Assert
        BusinessException ex = assertThrows(BusinessException.class, () -> {
            orderService.placeOrder(testUser.getEmail(), request);
        });

        assertEquals("CART_IS_EMPTY", ex.getErrorCode().name());
        verify(orderRepository, never()).save(any());
    }
}
