package org.example.homeandgarden.order.service;

import org.example.homeandgarden.order.dto.OrderCreateRequest;
import org.example.homeandgarden.order.dto.OrderResponse;
import org.example.homeandgarden.order.dto.OrderUpdateRequest;
import org.example.homeandgarden.shared.MessageResponse;
import org.springframework.data.domain.Page;

public interface OrderService {

    Page<OrderResponse> getUserOrders (String userId, Integer size, Integer page, String order, String sortBy);
    OrderResponse getOrderById(String orderId);
    MessageResponse getOrderStatus(String orderId);
    OrderResponse addOrder(OrderCreateRequest orderCreateRequest);
    OrderResponse updateOrder(String orderId, OrderUpdateRequest orderUpdateRequest);
    MessageResponse cancelOrder(String orderId);
    MessageResponse toggleOrderStatus(String orderId);

}
