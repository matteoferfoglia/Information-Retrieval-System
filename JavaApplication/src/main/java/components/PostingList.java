package components;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Class representing a {@link PostingList}.
 * @author Matteo Ferfoglia */
public class PostingList implements Serializable {

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
     * Creates a new instance of this class starting from a {@link List} of {@link Posting}s.
     * @param postings The list of {@link Posting}s. */
    private PostingList(@NotNull List<Posting> postings) {
        Objects.requireNonNull(postings, "The input argument cannot be null.");
        this.postings = postings;
        setSkipPointers();
    }

    /** Merges the {@link PostingList} given as parameter into this one (destructive merging).
     * Only distinct {@link Posting}s are kept (see {@link Posting#equals(Object)} and the
     * resulting {@link PostingList} (this one) is sorted.
     * @param other The other {@link PostingList} to be merged into this one. */
    public void merge(PostingList other) {
        if(other==null) {
            return;
        }
        this.postings.addAll(other.postings);
        this.postings = this.postings.parallelStream()
                            .distinct()
                            .sorted()
                            .collect(Collectors.toList());
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

        List<Posting> a_ = a.postings,
                      b_ = b.postings;
        ArrayList<Posting> union = new ArrayList<>(a_.size()+ b_.size());
        int i=0, j=0, comparison;
        while(i<a_.size() && j<b_.size()) {
            comparison = a_.get(i).compareTo(b_.get(j));
            if( comparison == 0 ) {
                union.add(a_.get(i++));
                j++;
            } else if (comparison < 0) {
                union.add(a_.get(i++));
            } else {
                union.add(b_.get(j++));
            }
        }
        union.addAll( a_.subList(i, a_.size()) );
        union.addAll(b_.subList(j, b_.size()) );
        union.trimToSize();

        return new PostingList(union);

        // TODO : positional union not implemented yet
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

        List<Posting> a_ = a.postings,
                      b_ = b.postings;

        ArrayList<Posting> intersection = new ArrayList<>(a_.size());
        int i=0, j=0, comparison;
        while(i<a_.size() && j<b_.size()) {
            comparison = a_.get(i).compareTo(b_.get(j));
            if( comparison == 0 ) {
                intersection.add(a_.get(i++));
                j++;
            } else if ( comparison < 0 ) {
                i++;
            } else {
                j++;
            }
        }
        intersection.trimToSize();

        return new PostingList(intersection);

        // TODO : positional intersect not implemented yet
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

    @Override
    public String toString() {
        return Arrays.toString(postings.toArray());
    }
}
