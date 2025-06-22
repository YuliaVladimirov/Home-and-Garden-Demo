package org.example.homeandgarden.order.mapper;

import lombok.RequiredArgsConstructor;
import org.example.homeandgarden.order.dto.OrderCreateRequest;
import org.example.homeandgarden.order.dto.OrderResponse;
import org.example.homeandgarden.order.entity.Order;
import org.example.homeandgarden.order.entity.enums.DeliveryMethod;
import org.example.homeandgarden.user.entity.User;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderMapper {

    public Order orderRequestToOrder(
            OrderCreateRequest orderCreateRequest,
            User user) {

        return Order.builder()
                .firstName(orderCreateRequest.getFirstName())
                .lastName(orderCreateRequest.getLastName())
                .address(orderCreateRequest.getAddress())
                .zipCode(orderCreateRequest.getZipCode())
                .city(orderCreateRequest.getCity())
                .phone(orderCreateRequest.getPhone())
                .deliveryMethod(DeliveryMethod.valueOf(orderCreateRequest.getDeliveryMethod()))
                .user(user)
                .build();
    }

    public OrderResponse orderToResponse(Order order) {

         return OrderResponse.builder()
                 .orderId(order.getOrderId())
                 .firstName(order.getFirstName())
                 .lastName(order.getLastName())
                 .address(order.getAddress())
                 .zipCode(order.getZipCode())
                 .city(order.getCity())
                 .phone(order.getPhone())
                 .deliveryMethod(order.getDeliveryMethod())
                 .orderStatus(order.getOrderStatus())
                 .createdAt(order.getCreatedAt())
                 .updatedAt(order.getUpdatedAt())
                 .build();
    }
}
