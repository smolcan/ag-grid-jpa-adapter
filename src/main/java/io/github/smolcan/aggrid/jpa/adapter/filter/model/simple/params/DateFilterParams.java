package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.SimpleFilterModelType;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Getter
@SuperBuilder
public class DateFilterParams extends ScalarFilterParams {

    @NonNull
    private final Set<SimpleFilterModelType> filterOptions;
    /**
     * @param maxValidDate the maximum valid date accepted by the filter (overrides {@code maxValidYear} if set).
     * @return the maximum valid date.
     */
    private final LocalDate maxValidDate;
    /**
     * @param maxValidYear the maximum valid year accepted by the filter.
     * @return the maximum valid year.
     */
    private final Integer maxValidYear;
    /**
     * @param minValidDate the minimum valid date accepted by the filter (overrides {@code minValidYear} if set).
     * @return the minimum valid date.
     */
    private final LocalDate minValidDate;
    /**
     * @param minValidYear the minimum valid year accepted by the filter.
     * @return the minimum valid year.
     */
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

        @NonNull
        public B filterOptions(@NonNull SimpleFilterModelType... type) {
            this.filterOptions = new HashSet<>(Arrays.asList(type));
            return self();
        }

        @NonNull
        public B filterOptions(@NonNull Collection<SimpleFilterModelType> type) {
            this.filterOptions = new HashSet<>(type);
            return self();
        }
    }
}
