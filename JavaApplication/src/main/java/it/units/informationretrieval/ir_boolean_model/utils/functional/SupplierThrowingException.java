package it.units.informationretrieval.ir_boolean_model.utils.functional;

import java.util.function.Supplier;

/**
 * This interface describes a {@link FunctionalInterface}
 * which can throw a {@link Throwable} object.
 *
 * @param <R> The output type.
 * @param <E> The subclass of {@link Throwable} for which a {@link Throwable}
 *            object can be thrown.
 * @author Matteo Ferfoglia
 */
@FunctionalInterface
public interface SupplierThrowingException<R, E extends Throwable> {

    /**
     * Similar to {@link Supplier#get()}, but this method
     * can throw {@link Throwable} objects.
     *
     * @throws E An exception {@link E} could be thrown.
     */
    R get() throws E;

}