package it.units.informationretrieval.ir_boolean_model.entities;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Class representing a {@link PostingList}.
 *
 * @author Matteo Ferfoglia
 */
public class PostingList implements Serializable {

    /**
     * The actual posting list.
     */
    @NotNull
    private List<Posting> postings;   // TODO: a built-in array may be faster but must be kept ordered

    /**
     * Constructor. Creates an empty {@link PostingList}.
     */
    public PostingList() {
        this(new ArrayList<>(0));
    }

    /**
     * Constructor.
     * Creates a new instance of this class starting from a {@link Posting}.
     *
     * @param posting The {@link Posting} used to create the new {@link PostingList}.
     *                Immediately after the creation, this will be the only-one
     *                element in the list.
     */
    public PostingList(@NotNull final Posting posting) {
        this(new ArrayList<>() {{
            add(Objects.requireNonNull(posting));
        }});
    }

    /**
     * Constructor.
     * Creates a new instance of this class starting from a {@link List} of {@link Posting}s.
     *
     * @param postings The list of {@link Posting}s.
     */
    public PostingList(@NotNull final List<Posting> postings) {  // TODO: test
        this.postings = Objects.requireNonNull(postings).stream().unordered().distinct().collect(Collectors.toList());
        setForwardPointers();
    }

    /**
     * Merges the {@link PostingList} given as parameter into this one (destructive merging).
     * Only distinct {@link Posting}s are kept (see {@link Posting#equals(Object)} and the
     * resulting {@link PostingList} (this one) is sorted.
     *
     * @param other The other {@link PostingList} to be merged into this one.
     */
    public void merge(@Nullable final PostingList other) {  // TODO: test
        if (other == null) {
            return;
        }
        this.postings.addAll(other.postings);
        sortAndRemoveDuplicates();
    }

    /**
     * Sets the forward pointers for this instance of {@link PostingList}.
     * &radic;p evenly spaced forward pointers will be created, with p = the
     * number of elements in this {@link PostingList}.
     */
    private void setForwardPointers() {    // TODO: test positions of forward pointers

        sortAndRemoveDuplicates();   // the postingList MUST be sorted

        final int numberOfPostings = postings.size();
        final int numberOfForwardPointers = (int) Math.round(Math.sqrt(numberOfPostings));
        int forwardPointerPositionPrevious = 0;
        if (numberOfForwardPointers > 0) {
            final int forwardPointerStep = numberOfPostings / numberOfForwardPointers; // The distance between two successive forwardPointers
            int[] forwardPointerPositions = IntStream.range(1, numberOfPostings)
                    .filter(x -> x % forwardPointerStep == 0)
                    .toArray();

            // Set the forwardPointers
            for (int forwardPointerPosition : forwardPointerPositions) {    // TODO : can be parallelized
                for (int j = forwardPointerPositionPrevious; j < forwardPointerPosition; j++) {
                    postings.get(j).setForwardPointer(postings.get(forwardPointerPosition));
                }
                forwardPointerPositionPrevious = forwardPointerPosition;
            }
        }
        // Set null the forwardPointers for the posting over the last forwardPointer
        for (; forwardPointerPositionPrevious < numberOfPostings; forwardPointerPositionPrevious++) {
            postings.get(forwardPointerPositionPrevious).setForwardPointer(null);
        }

    }

    /**
     * @return This instance as unmodifiable {@link List} of {@link Posting}s.
     */
    @NotNull
    public List<Posting> toListOfPostings() {
        return Collections.unmodifiableList(postings);
    }

    /**
     * Sort this instance.
     */
    private void sortAndRemoveDuplicates() {   // TODO: benchmark
        postings = postings.stream().sorted().distinct().collect(Collectors.toList());
    }

    /**
     * @return The size of this {@link PostingList}.
     */
    public int size() {
        return postings.size();
    }

    @Override
    public String toString() {
        return Arrays.toString(postings.toArray());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PostingList that = (PostingList) o;

        return postings.equals(that.postings);
    }

    @Override
    public int hashCode() {
        return postings.hashCode();
    }
}
