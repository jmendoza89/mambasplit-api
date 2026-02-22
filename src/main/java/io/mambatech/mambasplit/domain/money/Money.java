package io.mambatech.mambasplit.domain.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Money stored as minor units (cents) to avoid floating point errors.
 * Single-currency MVP: defaultCurrency is used everywhere.
 */
public final class Money implements Comparable<Money> {
    public static final String DEFAULT_CURRENCY = "USD";
    private static final int SCALE = 2;

    private final long amountCents;
    private final String currency;

    private Money(long amountCents, String currency) {
        this.amountCents = amountCents;
        this.currency = Objects.requireNonNull(currency);
    }

    public static Money ofCents(long cents) {
        return new Money(cents, DEFAULT_CURRENCY);
    }

    public static Money ofDecimal(BigDecimal amount) {
        Objects.requireNonNull(amount);
        BigDecimal scaled = amount.setScale(SCALE, RoundingMode.HALF_UP);
        long cents = scaled.movePointRight(SCALE).longValueExact();
        return new Money(cents, DEFAULT_CURRENCY);
    }

    public long cents() {
        return amountCents;
    }

    public BigDecimal toDecimal() {
        return BigDecimal.valueOf(amountCents, SCALE);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.addExact(this.amountCents, other.amountCents), currency);
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        return new Money(Math.subtractExact(this.amountCents, other.amountCents), currency);
    }

    private void requireSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Currency mismatch: " + this.currency + " vs " + other.currency);
        }
    }

    @Override
    public int compareTo(Money o) {
        requireSameCurrency(o);
        return Long.compare(this.amountCents, o.amountCents);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return amountCents == money.amountCents && currency.equals(money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amountCents, currency);
    }

    @Override
    public String toString() {
        return currency + " " + toDecimal();
    }
}
