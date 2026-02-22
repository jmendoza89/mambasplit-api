package io.mambatech.mambasplit.persistence.jpa;

import io.mambatech.mambasplit.domain.money.Money;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class MoneyCentsConverter implements AttributeConverter<Money, Long> {

    @Override
    public Long convertToDatabaseColumn(Money attribute) {
        if (attribute == null) return null;
        return attribute.cents();
    }

    @Override
    public Money convertToEntityAttribute(Long dbData) {
        if (dbData == null) return null;
        return Money.ofCents(dbData);
    }
}
