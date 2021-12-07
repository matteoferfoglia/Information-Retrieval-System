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
        Objects.requireNonNull(posting, "The posting cannot be null");
        this.postings = new ArrayList<>();
        this.postings.add(posting);
    }

    /**
     * Constructor.
     * Creates a new instance of this class starting from a {@link List} of {@link Posting}s.
     *
     * @param postings The list of {@link Posting}s.
     */
    private PostingList(@NotNull final List<Posting> postings) {  // TODO: test
        Objects.requireNonNull(postings, "The input argument cannot be null.");
        this.postings = postings.stream().unordered().distinct().collect(Collectors.toList());
        setSkipPointers();
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
        sort();
    }

    /**
     * Sets the skip pointers for this instance of {@link PostingList}.
     * &radic;p evenly spaced skip pointers will be created, with p = the
     * number of elements in this {@link PostingList}.
     */
    private void setSkipPointers() {    // TODO: test positions of skip pointers

        sort();   // the postingList MUST be sorted

        final int numberOfPostings = postings.size();
        final int numberOfSkipPointers = (int) Math.round(Math.sqrt(numberOfPostings));
        int skipPointerPositionPrevious = 0;
        if (numberOfSkipPointers > 0) {
            final int skipPointerStep = numberOfPostings / numberOfSkipPointers; // The distance between two successive skipPointers
            int[] skipPointerPositions = IntStream.range(1, numberOfPostings)
                    .filter(x -> x % skipPointerStep == 0)
                    .toArray();

            // Set the skipPointers
            for (int skipPointerPosition : skipPointerPositions) {    // TODO : can be parallelized
                for (int j = skipPointerPositionPrevious; j < skipPointerPosition; j++) {
                    postings.get(j).setForwardPointer(postings.get(skipPointerPosition));
                }
                skipPointerPositionPrevious = skipPointerPosition;
            }
        }
        // Set null the skipPointers for the posting over the last skipPointer
        for (; skipPointerPositionPrevious < numberOfPostings; skipPointerPositionPrevious++) {
            postings.get(skipPointerPositionPrevious).setForwardPointer(null);
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
    private void sort() {   // TODO: benchmark
        postings = postings.stream().sequential().sorted().collect(Collectors.toList());
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
}
