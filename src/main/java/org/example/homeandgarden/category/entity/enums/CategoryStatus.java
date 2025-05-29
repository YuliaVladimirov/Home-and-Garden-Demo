package org.example.homeandgarden.category.entity.enums;

import lombok.Getter;

@Getter
public enum CategoryStatus {
    ACTIVE ("Active"),
    INACTIVE ("Inactive");

    private final String value;

    CategoryStatus(String value) {
        this.value = value;
    }
}
