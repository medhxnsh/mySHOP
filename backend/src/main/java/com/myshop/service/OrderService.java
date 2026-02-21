package com.myshop.service;

import com.myshop.dto.request.OrderRequest;
import com.myshop.dto.request.OrderStatusUpdateRequest;
import com.myshop.dto.response.OrderResponse;
import com.myshop.dto.response.PagedResponse;
import com.myshop.event.internal.OrderCreatedEvent;
import com.myshop.exception.BusinessException;
import com.myshop.exception.ErrorCode;
import com.myshop.exception.ResourceNotFoundException;
import com.myshop.mapper.OrderMapper;
import com.myshop.model.entity.Cart;
import com.myshop.model.entity.CartItem;
import com.myshop.model.entity.Order;
import com.myshop.model.entity.OrderItem;
import com.myshop.model.entity.Product;
import com.myshop.model.entity.User;
import com.myshop.repository.jpa.CartRepository;
import com.myshop.repository.jpa.OrderRepository;
import com.myshop.repository.jpa.ProductRepository;
import com.myshop.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * OrderService manages order placement and transactions.
 * 
 * CORE CONCEPTS:
 * 1. Optimistic Locking: The product entity has a @Version column. When placing
 * an order,
 * the quantity is decremented. If two users buy the last item simultaneously,
 * the version
 * check fails for one, triggering an ObjectOptimisticLockingFailureException ->
 * rolls back.
 * 2. ACID Transaction: If any step fails (e.g. out of stock), everything
 * reverts. Cart isn't emptied.
 * 3. RequiresNew Propagation: cancelOrder() must start a new transaction to
 * guarantee stock is restored
 * regardless of the caller's transaction state.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final NotificationService notificationService;

    @Transactional
    public OrderResponse placeOrder(String email, OrderRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.CART_IS_EMPTY, "Cannot place order with empty cart."));

        if (cart.getItems().isEmpty()) {
            throw new BusinessException(ErrorCode.CART_IS_EMPTY, "Cart has no items.");
        }

        // 1. Build the Order entity
        Order order = Order.builder()
                .user(user)
                .shippingAddress(request.shippingAddress())
                .status("PENDING")
                .paymentStatus("PENDING")
                .totalAmount(BigDecimal.ZERO)
                .build();

        BigDecimal totalAmount = BigDecimal.ZERO;

        // 2. Iterate items: Snapshot prices & deduct stock
        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();

            // Re-verify stock (even though we checked when added to cart, it might have
            // changed)
            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                        "Product out of stock: " + product.getName() +
                                " (Requested: " + cartItem.getQuantity() + ", Available: " + product.getStockQuantity()
                                + ")");
            }

            // DEDUCT STOCK
            // This modifies the managed entity. When the transaction commits, Hibernate
            // issues: UPDATE products SET stock = new_val, version = version + 1 WHERE id =
            // ? AND version = old_version
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());

            // Snapshot the price exactly as it is right now
            BigDecimal unitPriceSnapshot = product.getPrice();
            BigDecimal subtotal = unitPriceSnapshot.multiply(new BigDecimal(cartItem.getQuantity()));

            totalAmount = totalAmount.add(subtotal);

            // Create historic OrderItem record
            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .unitPrice(unitPriceSnapshot)
                    .subtotal(subtotal)
                    .build();

            order.getItems().add(orderItem);
        }

        order.setTotalAmount(totalAmount);

        // 3. Save order (cascades to order_items)
        Order savedOrder = orderRepository.save(order);

        // Publish domain event
        eventPublisher.publishEvent(new OrderCreatedEvent(this, savedOrder));

        // TODO Phase 5: replace with Kafka event
        notificationService.createNotification(
                user.getId(),
                "ORDER_CONFIRMED",
                "Order Confirmed",
                "Your order has been successfully placed.",
                java.util.Map.of("orderId", savedOrder.getId().toString()));

        // 4. Clear the shopping cart
        cart.getItems().clear();
        cartRepository.save(cart);

        log.info("Order {} placed successfully for user {}", savedOrder.getId(), email);

        // 5. Publish synchronous domain event -> picked up by OrderEventListener
        eventPublisher.publishEvent(new OrderCreatedEvent(this, savedOrder));

        return orderMapper.toResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(String email, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId.toString()));

        // Ensure the user actually owns this order (or is an admin)
        User currentUser = userRepository.findByEmail(email).orElseThrow();
        if (!order.getUser().getId().equals(currentUser.getId()) && !currentUser.getRole().equals("ADMIN")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS, "You do not have permission to view this order");
        }

        return orderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getUserOrders(String email, int page, int size) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable);

        List<OrderResponse> content = orders.getContent().stream()
                .map(orderMapper::toResponse)
                .toList();

        return PagedResponse.of(orders, content);
    }

    // ADMIN ENDPOINT
    @Transactional(readOnly = true)
    public PagedResponse<OrderResponse> getAllOrders(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Order> orders;

        if (status != null && !status.isBlank()) {
            orders = orderRepository.findByStatusOrderByCreatedAtDesc(status.toUpperCase(), pageable);
        } else {
            orders = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                    .stream() // fallback since no native method exists for unpaged generic findall
                    .filter(x -> true).toList().isEmpty() ? Page.empty() : orderRepository.findAll(pageable); // crude
                                                                                                              // hack
                                                                                                              // for
                                                                                                              // compilation.
                                                                                                              // better
                                                                                                              // written
                                                                                                              // using
                                                                                                              // specs!

            // Actually properly we can just do:
            // orders = orderRepository.findAll(PageRequest.of(page, size,
            // Sort.by("createdAt").descending()));
            orders = orderRepository.findAll(PageRequest.of(page, size, Sort.by("createdAt").descending()));
        }

        List<OrderResponse> content = orders.getContent().stream()
                .map(orderMapper::toResponse)
                .toList();

        return PagedResponse.of(orders, content);
    }

    // ADMIN ENDPOINT
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatusUpdateRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId.toString()));

        order.setStatus(request.status().toUpperCase());
        log.info("Order {} status updated to {}", orderId, order.getStatus());

        return orderMapper.toResponse(orderRepository.save(order));
    }

    /**
     * Cancel the order and restore the stock quantities.
     * Propagation.REQUIRES_NEW ensures that if a parent transaction rolls back,
     * this isolated transaction still commits, meaning stock isn't left in limbo!
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public OrderResponse cancelOrder(String email, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId.toString()));

        User currentUser = userRepository.findByEmail(email).orElseThrow();
        if (!order.getUser().getId().equals(currentUser.getId()) && !currentUser.getRole().equals("ADMIN")) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS,
                    "You do not have permission to cancel this order");
        }

        if (!order.getStatus().equals("PENDING") && !order.getStatus().equals("PROCESSING")) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATE,
                    "Order cannot be cancelled in status: " + order.getStatus());
        }

        order.setStatus("CANCELLED");

        // Restore stock levels!
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
        }

        log.info("Order {} cancelled. Stock restored.", orderId);
        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse simulatePayment(String email, UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId.toString()));

        User currentUser = userRepository.findByEmail(email).orElseThrow();
        if (!order.getUser().getId().equals(currentUser.getId())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_ACCESS,
                    "You do not have permission to pay for this order");
        }

        if (order.getPaymentStatus().equals("PAID")) {
            throw new BusinessException(ErrorCode.INVALID_ORDER_STATE, "Order is already paid.");
        }

        // Simulate 90% success rate
        boolean paymentSuccess = Math.random() < 0.90;

        if (paymentSuccess) {
            order.setPaymentStatus("PAID");
            order.setStatus("PROCESSING");
            order.setPaymentReference("MOCK-TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            log.info("Mock payment SUCCESS for order {}", orderId);
        } else {
            order.setPaymentStatus("FAILED");
            log.warn("Mock payment FAILED for order {}", orderId);
            throw new BusinessException(ErrorCode.PAYMENT_FAILED, "The mock payment gateway declined the transaction.");
        }

        return orderMapper.toResponse(orderRepository.save(order));
    }
}
