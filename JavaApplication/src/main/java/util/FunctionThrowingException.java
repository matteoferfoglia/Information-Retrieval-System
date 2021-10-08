package util;

import java.util.function.Function;

/**
 * This interface describes a {@link FunctionalInterface}
 * which can throw a {@link Throwable} object.
 *
 * @param <T> The input type.
 * @param <R> The output type.
 * @param <E> The subclass of {@link Throwable} for which a {@link Throwable}
 *            object can be thrown.
 * @author Matteo Ferfoglia
 */
@FunctionalInterface
public interface FunctionThrowingException<T, R, E extends Throwable> {

    /**
     * Similar to {@link Function#apply(Object)}, but this method
     * can throws {@link Throwable} objects.
     *
     * @param a The input object.
     */
    R apply(T a) throws E;

}