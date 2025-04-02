package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params;

import java.time.LocalDate;

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
    }
}
