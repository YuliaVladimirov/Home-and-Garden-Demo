package org.example.homeandgarden.product.entity.enums;

import lombok.Getter;

@Getter
public enum ProductStatus {
    AVAILABLE ("Available"),
    OUT_OF_STOCK ("Out of Stock"),
    SOLD_OUT ("Sold Out");

    private final String value;

    ProductStatus(String value) {
        this.value = value;
    }
}
