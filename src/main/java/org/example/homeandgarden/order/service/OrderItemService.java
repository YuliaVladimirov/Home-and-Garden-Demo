package org.example.homeandgarden.order.service;

import org.example.homeandgarden.order.dto.OrderItemResponse;
import org.springframework.data.web.PagedModel;

public interface OrderItemService {

    PagedModel<OrderItemResponse>  getOrderItems(String orderId, Integer size, Integer page, String order, String sortBy);
}
