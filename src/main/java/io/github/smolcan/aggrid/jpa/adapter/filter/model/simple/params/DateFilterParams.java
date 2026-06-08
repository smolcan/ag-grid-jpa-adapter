package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.SimpleFilterModelType;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Getter
@SuperBuilder
public class DateFilterParams extends ScalarFilterParams {

    private final Set<SimpleFilterModelType> filterOptions;
    // The maximum valid date that can be entered in the filter.
    // If set, this will override `maxValidYear` - the maximum valid year setting.
    private final LocalDate maxValidDate;
    // This is the maximum year that may be entered in a date field for the value to be considered valid.
    private final Integer maxValidYear;
    // The minimum valid date that can be entered in the filter.
    // If set, this will override `minValidYear` - the minimum valid year setting.
    private final LocalDate minValidDate;
    // This is the minimum year that may be entered in a date field for the value to be considered valid.
    private final Integer minValidYear;

    public void validateDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return;
        }
        
        LocalDate date = dateTime.toLocalDate();
        this.validateDate(date);
    }
    
    public void validateDate(LocalDate date) {
        if (date == null) {
            return;
        }

        int year = date.getYear();
        if (this.maxValidDate != null) {
            if (date.isAfter(this.maxValidDate)) {
                throw new IllegalArgumentException("Max valid date exceeded");
            }
        } else if (this.maxValidYear != null) {
            if (year > this.maxValidYear) {
                throw new IllegalArgumentException("Max valid year exceeded!");
            }
        }

        if (this.minValidDate != null) {
            if (date.isBefore(this.minValidDate)) {
                throw new IllegalArgumentException("Min valid date exceeded");
            }
        } else if (this.minValidYear != null) {
            if (year < this.minValidYear) {
                throw new IllegalArgumentException("Min valid year exceeded!");
            }
        }
    }

    public abstract static class DateFilterParamsBuilder<C extends DateFilterParams, B extends DateFilterParamsBuilder<C, B>> extends ScalarFilterParamsBuilder<C, B> {
        // Hand-declared so Lombok keeps these defaults (ColDef-style) instead of using @Builder.Default.
        private Set<SimpleFilterModelType> filterOptions = Collections.emptySet();
        private Integer minValidYear = 1000;

        public B filterOptions(SimpleFilterModelType... type) {
            this.filterOptions = new HashSet<>(Arrays.asList(type));
            return self();
        }

        public B filterOptions(Collection<SimpleFilterModelType> type) {
            this.filterOptions = new HashSet<>(type);
            return self();
        }
    }
}
