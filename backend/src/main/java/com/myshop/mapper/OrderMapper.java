package com.myshop.mapper;

import com.myshop.dto.response.OrderItemResponse;
import com.myshop.dto.response.OrderResponse;
import com.myshop.model.entity.Order;
import com.myshop.model.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(target = "userId", source = "user.id")
    OrderResponse toResponse(Order order);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.name")
    OrderItemResponse toResponse(OrderItem orderItem);
}
