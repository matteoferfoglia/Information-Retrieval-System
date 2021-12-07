package it.units.informationretrieval.ir_boolean_model.entities;

import it.units.informationretrieval.ir_boolean_model.utils.Pair;
import it.units.informationretrieval.ir_boolean_model.utils.Summable;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

/**
 * Class representing a ranked subcontent.
 */
public abstract class DocumentRankedSubcontent
        implements Comparable<DocumentRankedSubcontent>, Summable<DocumentRankedSubcontent>/* TODO: rethink about this */, Serializable {
    /**
     * The ranked subcontent.
     */
    private final Pair<@NotNull DocumentContentRank, @NotNull String> rankedSubcontent;

    /**
     * Constructor.
     *
     * @param rank       The rank for this subcontent.
     * @param subcontent The subcontent.
     */
    public DocumentRankedSubcontent(@NotNull DocumentContentRank rank, @NotNull String subcontent) {
        this.rankedSubcontent = new Pair<>(Objects.requireNonNull(rank), Objects.requireNonNull(subcontent));
    }

    /**
     * @return the rank of this instance.
     */
    @NotNull
    public DocumentContentRank getRank() {
        return rankedSubcontent.getKey();
    }

    /**
     * @return the content of this instance.
     */
    @NotNull
    public String getContent() {
        return rankedSubcontent.getValue();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentRankedSubcontent that = (DocumentRankedSubcontent) o;
        return rankedSubcontent.equals(that.rankedSubcontent);
    }

    @Override
    public int hashCode() {
        return rankedSubcontent.hashCode();
    }

    @Override
    public String toString() {
        return getContent();
    }

    /**
     * Compare this instance with the given one.
     *
     * @param documentRankedSubcontent The other instance used for the comparison.
     */
    @Override
    public int compareTo(@NotNull DocumentRankedSubcontent documentRankedSubcontent) {
        return this.getRank().compareTo(documentRankedSubcontent.getRank());
    }

}