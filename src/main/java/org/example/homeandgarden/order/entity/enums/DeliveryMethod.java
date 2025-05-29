package org.example.homeandgarden.order.entity.enums;

import lombok.Getter;

@Getter
public enum DeliveryMethod {
    COURIER_DELIVERY ("Courier Delivery"),
    CUSTOMER_PICKUP ("Customer Pickup");

    private final String value;

    DeliveryMethod(String value) {
        this.value = value;
    }

}
