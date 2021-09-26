package components;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Class representing a {@link PostingList}.
 * @author Matteo Ferfoglia */
public class PostingList {

    /** The actual posting list. */
    @NotNull
    private List<Posting> postings;   // TODO: a built-in array may be faster but must be kept ordered

    /** Constructor.
     * Creates a new instance of this class starting from a {@link Posting}.
     * @param posting The {@link Posting} used to create the new {@link PostingList}.
     *                Immediately after the creation, this will be the only-one
     *                element in the list.
     */
    public PostingList(@NotNull Posting posting) {
        Objects.requireNonNull(posting, "The posting cannot be null");
        this.postings = new ArrayList<>();
        this.postings.add(posting);
    }

    /** Constructor.
     * Creates a new instance of this class starting from an array of {@link Posting}s.
     * @param postings The array of {@link Posting}s. */
    private PostingList(@NotNull Posting[] postings) {
        Objects.requireNonNull(postings, "The input argument cannot be null.");
        this.postings = Arrays.asList(postings);
        setSkipPointers();
    }

    /** Merges the {@link PostingList} given as parameter into this one (destructive merging).
     * @param other The other {@link PostingList} to be merged into this one. */
    public void merge(@NotNull PostingList other) {
        Objects.requireNonNull(other, "Cannot merge with null.");
        throw new UnsupportedOperationException("Not implemented yet"); // TODO : not implemented yet
    }

    /** Returns a new instance of {@link PostingList} which is the union of the
     * two {@link PostingList}s given as parameters.
     * @param a A {@link PostingList}.
     * @param b The other {@link PostingList}.
     * @return a new instance of {@link PostingList} which is the union of the
     *         two given {@link PostingList}s as parameters.*/
    @NotNull
    public static PostingList union(@NotNull PostingList a, @NotNull PostingList b) {
        Objects.requireNonNull(a, "Input Parameter cannot be null.");
        Objects.requireNonNull(b, "Input Parameter cannot be null.");
        throw new UnsupportedOperationException("Not implemented yet"); // TODO : not implemented yet
    }

    /** Returns a new instance of {@link PostingList} which is the intersection of the
     * two {@link PostingList}s given as parameters.
     * @param a A {@link PostingList}.
     * @param b The other {@link PostingList}.
     * @return a new instance of {@link PostingList} which is the union of the
     *         two given {@link PostingList}s as parameters.*/
    @NotNull
    public static PostingList intersection(@NotNull PostingList a, @NotNull PostingList b) {
        Objects.requireNonNull(a, "Input Parameter cannot be null.");
        Objects.requireNonNull(b, "Input Parameter cannot be null.");
        throw new UnsupportedOperationException("Not implemented yet"); // TODO : not implemented yet
    }

    /** Sets the skip pointers for this instance of {@link PostingList}.
     * &radic;p evenly spaced skip pointers will be created, with p = the
     * number of elements in this {@link PostingList}. */
    private void setSkipPointers() {

        sort();   // the postingList MUST be sorted

        final int numberOfPostings = postings.size();
        final int numberOfSkipPointers = (int) Math.round(Math.sqrt(numberOfPostings));
        final int skipPointerStep = numberOfPostings / numberOfSkipPointers; // The distance between two successive skipPointers
        int[] skipPointerPositions = IntStream.range(1, numberOfPostings)
                                              .filter(x -> x % skipPointerStep == 0)
                                              .toArray();

        // Set the skipPointers
        int skipPointerPositionPrevious = 0;
        for (int skipPointerPosition : skipPointerPositions) {    // TODO : can be parallelized
            for (int j = skipPointerPositionPrevious; j < skipPointerPosition; j++) {
                postings.get(j).setSkipPointer( postings.get(skipPointerPosition) );
            }
            skipPointerPositionPrevious = skipPointerPosition;
        }
        // Set null the skipPointers for the posting over the last skipPointer
        for( ; skipPointerPositionPrevious<numberOfPostings; skipPointerPositionPrevious++) {
            postings.get(skipPointerPositionPrevious).setSkipPointer( null );
        }

    }

    /** Sort this instance.*/
    private void sort() {
        postings = postings.stream().sorted().collect(Collectors.toList());
    }

    /** @return The size of this {@link PostingList}. */
    public int size() {
        return postings.size();
    }
}
