package org.example.homeandgarden.order.service;

import org.example.homeandgarden.cart.entity.CartItem;
import org.example.homeandgarden.cart.repository.CartRepository;
import org.example.homeandgarden.order.dto.*;
import org.example.homeandgarden.order.entity.Order;
import org.example.homeandgarden.order.entity.OrderItem;
import org.example.homeandgarden.order.entity.enums.DeliveryMethod;
import org.example.homeandgarden.order.entity.enums.OrderStatus;
import org.example.homeandgarden.order.mapper.OrderItemMapper;
import org.example.homeandgarden.order.mapper.OrderMapper;
import org.example.homeandgarden.order.repository.OrderItemRepository;
import org.example.homeandgarden.order.repository.OrderRepository;
import org.example.homeandgarden.exception.*;
import lombok.RequiredArgsConstructor;
import org.example.homeandgarden.shared.MessageResponse;
import org.example.homeandgarden.user.entity.User;
import org.example.homeandgarden.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PagedModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService{

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;

    private final CartRepository cartRepository;

    @Override
    public PagedModel<OrderResponse> getUserOrders(String userId, Integer size, Integer page, String order, String sortBy) {
        UUID id = UUID.fromString(userId);
        if (!userRepository.existsByUserId(id)) {
            throw new DataNotFoundException(String.format("User with id: %s, was not found.", userId));
        }
        PageRequest pageRequest = PageRequest.of(page, size, Sort.Direction.fromString(order), sortBy);
        Page<Order> orderPage = orderRepository.findByUserUserId(id, pageRequest);

        return new PagedModel<>(orderPage.map(orderMapper::orderToResponse));
    }

    @Override
    public OrderResponse getOrderById(String orderId) {

        UUID id = UUID.fromString(orderId);
        Order existingOrder = orderRepository.findById(id).orElseThrow(() -> new DataNotFoundException(String.format("Order with id: %s, was not found.", orderId)));
        return orderMapper.orderToResponse(existingOrder);
    }

    @Override
    public MessageResponse getOrderStatus(String orderId) {

        UUID id = UUID.fromString(orderId);
        Order existingOrder = orderRepository.findById(id).orElseThrow(() -> new DataNotFoundException(String.format("Order with id: %s, was not found.", orderId)));

        return MessageResponse.builder()
                .message(String.format("Order with id: %s has status '%s'.", orderId, existingOrder.getOrderStatus().name()))
                .build();
    }

    @Override
    @Transactional
    public OrderResponse addOrder(OrderCreateRequest orderCreateRequest) {

        UUID id = UUID.fromString(orderCreateRequest.getUserId());
        User existingUser = userRepository.findById(id).orElseThrow(() -> new DataNotFoundException(String.format("User with id: %s, was not found.", orderCreateRequest.getUserId())));

        Order orderToAdd = orderMapper.orderRequestToOrder(orderCreateRequest, existingUser);
        Order addedOrder = orderRepository.saveAndFlush(orderToAdd);

        Set<CartItem> cart = existingUser.getCart();
        for (CartItem cartItem : cart) {
            OrderItem orderItemToAdd = orderItemMapper.cartItemToOrderItem(cartItem,addedOrder,cartItem.getProduct());

            addedOrder.getOrderItems().add(orderItemToAdd);
            orderItemRepository.saveAndFlush(orderItemToAdd);
        }

        cartRepository.deleteAll(cart);
        return orderMapper.orderToResponse(addedOrder);
    }

    @Override
    @Transactional
    public OrderResponse updateOrder(String orderId, OrderUpdateRequest orderUpdateRequest) {
        UUID id = UUID.fromString(orderId);
        Order existingOrder = orderRepository.findById(id).orElseThrow(() -> new DataNotFoundException(String.format("Order with id: %s, was not found.", orderId)));

        Set<OrderStatus> finalStatuses = EnumSet.of(
                OrderStatus.ON_THE_WAY,
                OrderStatus.DELIVERED,
                OrderStatus.CANCELED,
                OrderStatus.RETURNED
        );
        if (finalStatuses.contains(existingOrder.getOrderStatus())) {
            throw new IllegalArgumentException(String.format("Order with id: %s is already in status '%s' and can not be updated.", orderId, existingOrder.getOrderStatus().name()));
        }
        Optional.ofNullable(orderUpdateRequest.getAddress()).ifPresent(existingOrder::setAddress);
        Optional.ofNullable(orderUpdateRequest.getZipCode()).ifPresent(existingOrder::setZipCode);
        Optional.ofNullable(orderUpdateRequest.getCity()).ifPresent(existingOrder::setCity);
        Optional.ofNullable(orderUpdateRequest.getPhone()).ifPresent(existingOrder::setPhone);
        Optional.of(DeliveryMethod.valueOf(orderUpdateRequest.getDeliveryMethod())).ifPresent(existingOrder::setDeliveryMethod);

        Order updatedOrder = orderRepository.saveAndFlush(existingOrder);
        return orderMapper.orderToResponse(updatedOrder);
    }

    @Override
    @Transactional
    public MessageResponse cancelOrder(String orderId) {
        UUID id = UUID.fromString(orderId);
        Order existingOrder = orderRepository.findById(id).orElseThrow(() -> new DataNotFoundException(String.format("Order with id: %s, was not found.", orderId)));

        if (!existingOrder.getOrderStatus().equals(OrderStatus.CREATED)) {
            throw new IllegalArgumentException(String.format("Order with id: %s is already in status '%s' and can not be canceled.", orderId, existingOrder.getOrderStatus().name()));
        }

        existingOrder.setOrderStatus(OrderStatus.CANCELED);
        Order updatedOrder = orderRepository.saveAndFlush(existingOrder);

        return MessageResponse.builder()
                .message(String.format("Order with id: %s was canceled.", updatedOrder.getOrderId().toString()))
                .build();
    }

    @Override
    @Transactional
    public MessageResponse toggleOrderStatus(String orderId) {

        UUID id = UUID.fromString(orderId);
        Order existingOrder = orderRepository.findById(id).orElseThrow(() -> new DataNotFoundException(String.format("Order with id: %s, was not found.", orderId)));

        String initialStatus = existingOrder.getOrderStatus().name();

        Set<OrderStatus> finalStatuses = EnumSet.of(
                OrderStatus.CANCELED,
                OrderStatus.RETURNED
        );
        if (finalStatuses.contains(existingOrder.getOrderStatus())) {
            throw new IllegalArgumentException(String.format("Order with id: %s is in final status %s and the status can not be changed.", orderId, existingOrder.getOrderStatus().name()));
        }

        switch (existingOrder.getOrderStatus()) {
            case OrderStatus.CREATED -> existingOrder.setOrderStatus(OrderStatus.PAID);
            case OrderStatus.PAID -> existingOrder.setOrderStatus(OrderStatus.ON_THE_WAY);
            case ON_THE_WAY -> existingOrder.setOrderStatus(OrderStatus.DELIVERED);
            case DELIVERED -> existingOrder.setOrderStatus(OrderStatus.RETURNED);
        }

        Order updatedOrder = orderRepository.saveAndFlush(existingOrder);

        return MessageResponse.builder()
                .message(String.format("Order with id: %s was updated from status '%s' to status '%s'.", orderId, initialStatus, updatedOrder.getOrderStatus().name()))
                .build();
    }
}

