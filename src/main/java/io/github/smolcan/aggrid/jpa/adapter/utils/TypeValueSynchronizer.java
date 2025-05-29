package io.github.smolcan.aggrid.jpa.adapter.utils;


import jakarta.persistence.criteria.Expression;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


/**
 * Before comparing expression with value in criteria API, need to make sure they are compatible and minimize runtime errors
 */
public class TypeValueSynchronizer {


    /**
     * Synchronizes the types of the expression and value provided for a query,
     * ensuring that the value is converted to the appropriate type for comparison
     * based on the expression's expected type.
     * This method handles a variety of data types including:
     * <ul>
     *     <li>String</li>
     *     <li>LocalDate</li>
     *     <li>Number types (converted to BigDecimal)</li>
     *     <li>Boolean types</li>
     * </ul>
     *
     * The method returns a Result object containing the converted expression and value.
     * The value is parsed from the String representation into the appropriate type
     * based on the type of the expression.
     * If the type of the expression is unsupported, an {@link IllegalArgumentException}
     * is thrown.
     *
     * @param expr The expression to be synchronized, representing the expected type.
     * @param value The String value to be converted to the appropriate type for comparison.
     * @return A Result object containing the expression and the converted value, both of which are compatible.
     * @throws IllegalArgumentException if the type of the expression is not supported or if parsing fails.
     */
    public static Result<?> synchronizeTypes(Expression<?> expr, String value) {
        if (value == null) {
            return new Result<>(expr, null);
        }
        
        Class<?> exprType = expr.getJavaType();
        
        // compatible
        if (exprType.equals(String.class)) {
            return new Result<>(expr.as(String.class), value);
        }

        // date type (ag grid supports format yyyy-mm-dd, so only LocalDate allowed in default configuration
        if (LocalDate.class.equals(exprType)) {
            return new Result<>(
                    expr.as(LocalDate.class),
                    LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
            );
        }
        
        // number type
        if (Number.class.isAssignableFrom(exprType)) {
            // can make them both big decimal and comparable
            return new Result<>(
                    expr.as(BigDecimal.class),
                    new BigDecimal(value)
            );
        }

        // boolean
        if (exprType.equals(Boolean.class)) {
            return new Result<>(
                    expr.as(Boolean.class),
                    Boolean.parseBoolean(value)
            );
        }

        // idk wtf this field is, compare without check, universe might explode
        return new Result(expr, value);
    }


    public static class Result<E> {
        private final Expression<E> synchronizedPath;
        private final E synchronizedValue;
        
        public Result(Expression<E> synchronizedPath, E synchronizedValue) {
            this.synchronizedPath = synchronizedPath;
            this.synchronizedValue = synchronizedValue;
        }

        public Expression<?> getSynchronizedPath() {
            return synchronizedPath;
        }

        public Object getSynchronizedValue() {
            return synchronizedValue;
        }
    }

}
