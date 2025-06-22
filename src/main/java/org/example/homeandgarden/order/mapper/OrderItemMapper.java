package org.example.homeandgarden.order.mapper;

import lombok.RequiredArgsConstructor;
import org.example.homeandgarden.cart.entity.CartItem;
import org.example.homeandgarden.order.dto.OrderItemResponse;
import org.example.homeandgarden.order.entity.Order;
import org.example.homeandgarden.order.entity.OrderItem;
import org.example.homeandgarden.product.dto.ProductResponse;
import org.example.homeandgarden.product.entity.Product;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderItemMapper {

    public OrderItem cartItemToOrderItem(
            CartItem cartItem,
            Order order,
            Product product) {

        return OrderItem.builder()
                .quantity(cartItem.getQuantity())
                .priceAtPurchase(product.getCurrentPrice())
                .order(order)
                .product(product)
                .build();
    }

    public OrderItemResponse orderItemToResponse(
            OrderItem orderItem,
            ProductResponse product) {

        return OrderItemResponse.builder()
                .orderItemId(orderItem.getOrderItemId())
                .quantity(orderItem.getQuantity())
                .priceAtPurchase(orderItem.getPriceAtPurchase())
                .product(product)
                .build();
    }
}
