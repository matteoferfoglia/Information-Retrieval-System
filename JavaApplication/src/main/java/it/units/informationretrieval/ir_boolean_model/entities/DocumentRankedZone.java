package it.units.informationretrieval.ir_boolean_model.entities;

import it.units.informationretrieval.ir_boolean_model.utils.Pair;
import it.units.informationretrieval.ir_boolean_model.utils.Summable;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

/**
 * Class representing a ranked zone of the {@link Document}.
 */
public abstract class DocumentRankedZone
        implements Comparable<DocumentRankedZone>, Summable<DocumentRankedZone>/* TODO: rethink about this */, Serializable {
    /**
     * The ranked subcontent.
     */
    private final Pair<@NotNull DocumentZoneRank, @NotNull String> rankedSubcontent;

    /**
     * Constructor.
     *
     * @param rank       The rank for this subcontent.
     * @param subcontent The subcontent.
     */
    public DocumentRankedZone(@NotNull DocumentZoneRank rank, @NotNull String subcontent) {
        this.rankedSubcontent = new Pair<>(Objects.requireNonNull(rank), Objects.requireNonNull(subcontent));
    }

    /**
     * @return the rank of this instance.
     */
    @NotNull
    public DocumentZoneRank getRank() {
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
        DocumentRankedZone that = (DocumentRankedZone) o;
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
     * @param documentRankedZone The other instance used for the comparison.
     */
    @Override
    public int compareTo(@NotNull DocumentRankedZone documentRankedZone) {
        return this.getRank().compareTo(documentRankedZone.getRank());
    }

}