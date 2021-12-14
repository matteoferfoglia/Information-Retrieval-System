package it.units.informationretrieval.ir_boolean_model.utils;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface Summable<T> {
    /**
     * @param t The other instance
     * @return the sum of this instance with the other.
     */
    int sum(@NotNull T t);

    /**
     * @param tCollection The collection of other instances to be sum to this one.
     * @return the sum of this instance with the sum of all the other.
     */
    int sum(@NotNull Collection<@NotNull T> tCollection);
}
