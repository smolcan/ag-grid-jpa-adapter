package io.github.smolcan.aggrid.jpa.adapter.utils;


import jakarta.persistence.criteria.Expression;
import lombok.NonNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;


/**
 * Before comparing expression with value in criteria API, need to make sure they are compatible and minimize runtime errors
 */
public class TypeValueSynchronizer {

    private TypeValueSynchronizer() {}

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
    @NonNull
    @SuppressWarnings({"unchecked", "rawtypes", "java:S1452"})
    public static Result<?> synchronizeTypes(@NonNull Expression<?> expr, String value) {
        if (value == null) {
            return new Result<>(expr, null);
        }
        
        Class<?> exprType = expr.getJavaType();
        if (exprType.isPrimitive()) {
            exprType = primitiveToWrapper(exprType);
        }

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

        // uuid
        if (UUID.class.equals(exprType)) {
            return new Result<>(
                    expr.as(UUID.class), 
                    UUID.fromString(value)
            );
        }
        
        // enum
        if (exprType.isEnum()) {
            Enum<?> enumValue = Enum.valueOf((Class<? extends Enum>) exprType, value);
            return new Result(
                    expr,
                    enumValue
            );
        }

        // idk wtf this field is, compare without check, universe might explode
        return new Result(expr, value);
    }
    

    private static Class<?> primitiveToWrapper(Class<?> type) {
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == short.class) return Short.class;
        if (type == byte.class) return Byte.class;
        if (type == boolean.class) return Boolean.class;
        if (type == char.class) return Character.class;
        return type;
    }


    public static class Result<E> {
        private final Expression<E> synchronizedPath;
        private final E synchronizedValue;
        
        public Result(@NonNull Expression<E> synchronizedPath, E synchronizedValue) {
            this.synchronizedPath = synchronizedPath;
            this.synchronizedValue = synchronizedValue;
        }

        @NonNull
        public Expression<?> getSynchronizedPath() {
            return synchronizedPath;
        }

        public Object getSynchronizedValue() {
            return synchronizedValue;
        }
    }

}
