package org.example.homeandgarden.order.entity.enums;

import lombok.Getter;

@Getter
public enum OrderStatus {
    CREATED ("Created"),
    PAID ("Paid"),
    ON_THE_WAY ("On the Way"),
    DELIVERED ("Delivered"),
    CANCELED ("Canceled"),
    RETURNED ("Returned");

    private final String value;

    OrderStatus(String value) {
        this.value = value;
    }

}
