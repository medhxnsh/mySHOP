package com.myshop.mapper;

import com.myshop.dto.response.CartItemResponse;
import com.myshop.dto.response.CartResponse;
import com.myshop.model.entity.Cart;
import com.myshop.model.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.List;

@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(target = "userId", source = "user.id")
    @Mapping(target = "totalAmount", expression = "java(calculateTotalAmount(cart.getItems()))")
    CartResponse toResponse(Cart cart);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    // Unit price is calculated dynamically for the cart from the current product
    // price
    @Mapping(target = "unitPrice", source = "product.price")
    @Mapping(target = "subtotal", expression = "java(cartItem.getProduct().getPrice().multiply(new java.math.BigDecimal(cartItem.getQuantity())))")
    CartItemResponse toResponse(CartItem cartItem);

    default BigDecimal calculateTotalAmount(List<CartItem> items) {
        if (items == null || items.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return items.stream()
                .map(item -> item.getProduct().getPrice().multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
