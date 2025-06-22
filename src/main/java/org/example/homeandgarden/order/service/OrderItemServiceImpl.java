package org.example.homeandgarden.order.service;

import lombok.RequiredArgsConstructor;
import org.example.homeandgarden.exception.DataNotFoundException;
import org.example.homeandgarden.order.dto.OrderItemResponse;
import org.example.homeandgarden.order.entity.OrderItem;
import org.example.homeandgarden.order.mapper.OrderItemMapper;
import org.example.homeandgarden.order.repository.OrderItemRepository;
import org.example.homeandgarden.order.repository.OrderRepository;
import org.example.homeandgarden.product.mapper.ProductMapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderItemServiceImpl implements OrderItemService {

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;

    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;

    @Override
    public PagedModel<OrderItemResponse> getOrderItems(String orderId, Integer size, Integer page, String order, String sortBy) {
        UUID id = UUID.fromString(orderId);
        if (!orderRepository.existsByOrderId(id)) {
            throw new DataNotFoundException(String.format("Order with id: %s, was not found.", orderId));
        }
        PageRequest pageRequest = PageRequest.of(page, size, Sort.Direction.fromString(order), sortBy);
        Page<OrderItem> orderItemPage = orderItemRepository.findByOrderOrderId(id, pageRequest);

        return new PagedModel<>(orderItemPage.map((item) -> orderItemMapper.orderItemToResponse(item,
                productMapper.productToResponse(item.getProduct()))));
    }
}
