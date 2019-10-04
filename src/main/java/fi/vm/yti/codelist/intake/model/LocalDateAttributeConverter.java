package fi.vm.yti.codelist.intake.model;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Optional;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

/**
 * This converter needs to exist because JPA 2.1 does not support LocalDate directly.
 * <p>
 * See for example here: https://www.thoughts-on-java.org/persist-localdate-localdatetime-jpa/
 * </p>
 */
@Converter
public class LocalDateAttributeConverter implements AttributeConverter<LocalDate, Date> {

    @Override
    public Date convertToDatabaseColumn(final LocalDate localDate) {
        return Optional.ofNullable(localDate)
            .map(Date::valueOf)
            .orElse(null);
    }

    @Override
    public LocalDate convertToEntityAttribute(final Date date) {
        return Optional.ofNullable(date)
            .map(Date::toLocalDate)
            .orElse(null);
    }
}