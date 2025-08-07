package org.example.homeandgarden.order.service;

import lombok.RequiredArgsConstructor;
import org.example.homeandgarden.exception.DataNotFoundException;
import org.example.homeandgarden.order.dto.OrderItemResponse;
import org.example.homeandgarden.order.entity.Order;
import org.example.homeandgarden.order.entity.OrderItem;
import org.example.homeandgarden.order.mapper.OrderItemMapper;
import org.example.homeandgarden.order.repository.OrderItemRepository;
import org.example.homeandgarden.order.repository.OrderRepository;
import org.example.homeandgarden.product.mapper.ProductMapper;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
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
    public Page<OrderItemResponse> getUserOrderItems(String orderId, Integer size, Integer page, String order, String sortBy) {

        UUID id = UUID.fromString(orderId);
        if (!orderRepository.existsByOrderId(id)) {
            throw new DataNotFoundException(String.format("Order with id: %s, was not found.", orderId));
        }
        PageRequest pageRequest = PageRequest.of(page, size, Sort.Direction.fromString(order), sortBy);
        Page<OrderItem> orderItemPage = orderItemRepository.findByOrderOrderId(id, pageRequest);

        return orderItemPage.map((item) -> orderItemMapper.orderItemToResponse(item,
                productMapper.productToResponse(item.getProduct())));
    }

    @Override
    public Page<OrderItemResponse> getMyOrderItems(String email, String orderId, Integer size, Integer page, String order, String sortBy) {

        UUID id = UUID.fromString(orderId);
        Order existingOrder = orderRepository.findById(id).orElseThrow(() -> new DataNotFoundException(String.format("Order with id: %s, was not found.", orderId)));

        if (!existingOrder.getUser().getEmail().equals(email)) {
            throw new AccessDeniedException(String.format("Order with id: %s, does not belong to the user with email: %s.", orderId, email));
        }

        PageRequest pageRequest = PageRequest.of(page, size, Sort.Direction.fromString(order), sortBy);
        Page<OrderItem> orderItemPage = orderItemRepository.findByOrderOrderId(id, pageRequest);

        return orderItemPage.map((item) -> orderItemMapper.orderItemToResponse(item,
                productMapper.productToResponse(item.getProduct())));
    }
}
