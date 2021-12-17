package it.units.informationretrieval.ir_boolean_model.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * Interface representing one element of a {@link SkipList}.
 *
 * @param <T> The type of the element.
 * @author Matteo Ferfoglia
 */
public interface SkipListElement<T> extends Serializable, Comparable<T> {

    /**
     * @param i The index of the forwarded element in the {@link SkipList}
     *          or -1 if you want to set the forward pointer to null.
     * @param e The {@link SkipListElement} to which this instance forwards.
     * @return this instance after the invocation of this method.
     */
    @NotNull
    SkipListElement<T> setForwardPointer(int i, @Nullable SkipListElement<T> e);

    /**
     * @return the instance forwarded by this object or null if this object has
     * not any forward pointers set.
     */
    @Nullable
    SkipListElement<T> getForwardedElement();

    /**
     * @return true if this instance has a forward pointer, false otherwise.
     */
    boolean hasForwardPointer();

    /**
     * @return the element.
     */
    T getElement();

    /**
     * @return the index in the {@link SkipList} to which this instance belongs of
     * the forwarded element pointed by this instance or -1 if this instance has
     * not a forward pointer.
     */
    int getForwardedIndex();
}
