package io.github.habatoo.dto.enums;

public enum Currency {
    RUB("Российский рубль"),
    USD("Американский доллар"),
    CNY("Китайский юань");

    private final String currency;

    Currency(String currency) {
        this.currency = currency;
    }
}
