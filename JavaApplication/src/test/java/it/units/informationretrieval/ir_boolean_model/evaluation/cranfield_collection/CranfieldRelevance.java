package it.units.informationretrieval.ir_boolean_model.evaluation.cranfield_collection;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Enumeration of possible value of relevance assigned to documents
 * from the Cranfield's collection.
 * Each enum value has its own value (used for classification in
 * input documents) and the corresponding description for the value.
 *
 * @author Matteo Ferfoglia
 */
public enum CranfieldRelevance {

    // enum values sorted by relevance
    COMPLETE_ANSWER(1, "References which are a complete answer to the question."),
    HIGH_RELEVANCE(2, "References of a high degree of relevance, the lack of which"
            + " either would have made the research impracticable or would"
            + "  have resulted in a considerable amount of extra work."),
    USEFUL(3, "References which were useful, either as general background"
            + " to the work or as suggesting methods of tackling certain aspects"
            + " of the work."),
    LOW_INTEREST(4, "References of minimum interest, for example, those that have been"
            + " included from an historical viewpoint."),
    NO_INTEREST(5, "References of no interest.");


    /**
     * Minimum value for {@link #relevance}.
     */
    private final static int MIN_RELEVANCE_VALUE = 1;
    /**
     * Maximum value for {@link #relevance}.
     */
    private final static int MAX_RELEVANCE_VALUE = 5;
    /**
     * Relevance numeric value.
     */
    @Range(from = MIN_RELEVANCE_VALUE, to = MAX_RELEVANCE_VALUE)
    private final int relevance;
    /**
     * Description for the enum value.
     */
    @NotNull
    private final String description;

    /**
     * Constructor.
     */
    CranfieldRelevance(int relevance, @NotNull String description) {
        this.relevance = relevance;
        this.description = Objects.requireNonNull(description);
        assert MIN_RELEVANCE_VALUE <= relevance && relevance <= MAX_RELEVANCE_VALUE;
    }

    /**
     * @param relevance The relevance, provided as numeric value.
     * @return the enum instance for the provided relevance value.
     * @throws NoSuchElementException if the input parameter is not a valid
     *                                value for {@link CranfieldRelevance}.
     */
    @NotNull
    public static CranfieldRelevance getEnumValueFromNumericRelevance(int relevance)
            throws NoSuchElementException {
        return Arrays.stream(values())
                .filter(enumVal -> enumVal.relevance == relevance)
                .findAny()
                .orElseThrow();
    }
}
