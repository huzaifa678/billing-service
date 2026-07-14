package com.project.billing.domain.shared;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {

    @Test
    void multiplyScalesAmountAndKeepsCurrency() {
        Money result = Money.of("10.00", "USD").multiply(3);
        assertEquals(0, result.amount().compareTo(new BigDecimal("30.00")));
        assertEquals("USD", result.currency());
    }

    @Test
    void divideByOneReturnsSameAmount() {
        Money result = Money.of("29.99", "usd").divide(1);
        assertEquals(0, result.amount().compareTo(new BigDecimal("29.99")));
        assertEquals("USD", result.currency());
    }

    @Test
    void rejectsInvalidCurrency() {
        assertThrows(IllegalArgumentException.class, () -> Money.of("1", "US"));
    }

    @Test
    void rejectsDivideByZero() {
        assertThrows(IllegalArgumentException.class, () -> Money.of("1", "USD").divide(0));
    }
}
