package org.example.homeandgarden.logging;
import org.slf4j.MDC;

public enum MDCFields {

    REQUEST_ID("REQUEST_ID"),
    LAYER("LAYER"),
    METHOD("METHOD"),
    DURATION("DURATION");

    private final String field;

    MDCFields(String field) {
        this.field = field;
    }

    public String getFieldName() {
        return field;
    }

    public void put(Object value) {
        MDC.put(field, String.valueOf(value));
    }

    public void remove() {
        MDC.remove(field);
    }
}

