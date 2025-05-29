package org.example.homeandgarden.user.entity.enums;

import lombok.Getter;

@Getter
public enum UserRole {
    CLIENT ("Client"),
    ADMINISTRATOR ("Administrator");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

}
