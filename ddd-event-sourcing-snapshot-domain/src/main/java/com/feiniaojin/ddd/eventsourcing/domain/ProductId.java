package com.feiniaojin.ddd.eventsourcing.domain;

public class ProductId {
    private String value;

    public ProductId() {
    }

    public ProductId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
