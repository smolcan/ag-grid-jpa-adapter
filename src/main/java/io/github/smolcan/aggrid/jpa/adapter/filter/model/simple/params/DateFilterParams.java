package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DateFilterParams extends ScalarFilterParams {

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
    
    private DateFilterParams(Builder builder) {
        super(builder);
        this.maxValidDate = builder.maxValidDate;
        this.maxValidYear = builder.maxValidYear;
        this.minValidDate = builder.minValidDate;
        this.minValidYear = builder.minValidYear;
    }
    
    public static Builder builder() {
        return new Builder();
    }

    public LocalDate getMaxValidDate() {
        return maxValidDate;
    }

    public Integer getMaxValidYear() {
        return maxValidYear;
    }

    public LocalDate getMinValidDate() {
        return minValidDate;
    }

    public Integer getMinValidYear() {
        return minValidYear;
    }

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

    public static class Builder extends ScalarFilterParams.Builder {

        private LocalDate maxValidDate;
        private Integer maxValidYear;
        private LocalDate minValidDate;
        private Integer minValidYear = 1000;
        
        public DateFilterParams build() {
            return new DateFilterParams(this);
        }
        
        public Builder maxValidDate(LocalDate maxValidDate) {
            this.maxValidDate = maxValidDate;
            return this;
        }
        
        public Builder maxValidYear(Integer maxValidYear) {
            this.maxValidYear = maxValidYear;
            return this;
        }
        
        public Builder minValidDate(LocalDate minValidDate) {
            this.minValidDate = minValidDate;
            return this;
        }
        
        public Builder minValidYear(Integer minValidYear) {
            this.minValidYear = minValidYear;
            return this;
        }

        @Override
        public Builder inRangeInclusive(boolean inRangeInclusive) {
            super.inRangeInclusive(inRangeInclusive);
            return this;
        }

        @Override
        public Builder includeBlanksInEquals(boolean includeBlanksInEquals) {
            super.includeBlanksInEquals(includeBlanksInEquals);
            return this;
        }

        @Override
        public Builder includeBlanksInNotEqual(boolean includeBlanksInNotEqual) {
            super.includeBlanksInNotEqual(includeBlanksInNotEqual);
            return this;
        }

        @Override
        public Builder includeBlanksInLessThan(boolean includeBlanksInLessThan) {
            super.includeBlanksInLessThan(includeBlanksInLessThan);
            return this;
        }

        @Override
        public Builder includeBlanksInGreaterThan(boolean includeBlanksInGreaterThan) {
            super.includeBlanksInGreaterThan(includeBlanksInGreaterThan);
            return this;
        }

        @Override
        public Builder includeBlanksInRange(boolean includeBlanksInRange) {
            super.includeBlanksInRange(includeBlanksInRange);
            return this;
        }
    }
}
