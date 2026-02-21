package com.myshop.service;

import com.myshop.dto.request.CartItemRequest;
import com.myshop.dto.response.CartResponse;
import com.myshop.exception.BusinessException;
import com.myshop.exception.ErrorCode;
import com.myshop.exception.ResourceNotFoundException;
import com.myshop.mapper.CartMapper;
import com.myshop.model.entity.Cart;
import com.myshop.model.entity.CartItem;
import com.myshop.model.entity.Product;
import com.myshop.model.entity.User;
import com.myshop.repository.jpa.CartItemRepository;
import com.myshop.repository.jpa.CartRepository;
import com.myshop.repository.jpa.ProductRepository;
import com.myshop.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;

    @Transactional
    public CartResponse getCartByUser(String email) {
        Cart cart = getOrCreateCart(email);
        return cartMapper.toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(String email, CartItemRequest request) {
        Cart cart = getOrCreateCart(email);

        Product product = productRepository.findById(request.productId())
                .filter(Product::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.productId().toString()));

        // Guard: check stock
        if (product.getStockQuantity() < request.quantity()) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                    "Not enough stock for product: " + product.getName() + ". Available: "
                            + product.getStockQuantity());
        }

        // Check if item already exists in cart
        Optional<CartItem> existingItemOpt = cartItemRepository.findByCartIdAndProductId(cart.getId(), product.getId());

        if (existingItemOpt.isPresent()) {
            CartItem existingItem = existingItemOpt.get();
            int newQuantity = existingItem.getQuantity() + request.quantity();

            if (product.getStockQuantity() < newQuantity) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                        "Not enough stock to add " + request.quantity() + " more of " + product.getName());
            }

            existingItem.setQuantity(newQuantity);
            log.debug("Updated cart item quantity for product: {}", product.getId());
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.quantity())
                    .build();
            cart.addItem(newItem);
            log.debug("Added new item to cart: {}", product.getId());
        }

        Cart savedCart = cartRepository.save(cart);
        return cartMapper.toResponse(savedCart);
    }

    @Transactional
    public CartResponse updateItemQuantity(String email, UUID productId, int quantity) {
        Cart cart = getOrCreateCart(email);

        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem for Product", "id", productId.toString()));

        Product product = cartItem.getProduct();

        if (product.getStockQuantity() < quantity) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                    "Not enough stock for product: " + product.getName());
        }

        cartItem.setQuantity(quantity);
        cartRepository.save(cart); // Cascades

        return cartMapper.toResponse(cart);
    }

    @Transactional
    public CartResponse removeItem(String email, UUID productId) {
        Cart cart = getOrCreateCart(email);

        CartItem cartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem for Product", "id", productId.toString()));

        cart.removeItem(cartItem);
        // Explicitly delete the orphan item
        cartItemRepository.delete(cartItem);

        Cart savedCart = cartRepository.save(cart);
        return cartMapper.toResponse(savedCart);
    }

    @Transactional
    public void clearCart(String email) {
        Cart cart = getOrCreateCart(email);
        cart.getItems().clear();
        cartRepository.save(cart);
    }

    // Helper method: get the cart or create an empty one if it doesn't exist
    private Cart getOrCreateCart(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        return cartRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Cart newCart = Cart.builder().user(user).build();
                    return cartRepository.save(newCart);
                });
    }
}
