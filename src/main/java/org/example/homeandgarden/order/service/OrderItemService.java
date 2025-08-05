package org.example.homeandgarden.order.service;

import org.example.homeandgarden.order.dto.OrderItemResponse;
import org.springframework.data.domain.Page;

public interface OrderItemService {

    Page<OrderItemResponse> getUserOrderItems(String orderId, Integer size, Integer page, String order, String sortBy);
}
