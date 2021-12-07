package it.units.informationretrieval.ir_boolean_model.entities.document;

import it.units.informationretrieval.ir_boolean_model.utils.Pair;
import it.units.informationretrieval.ir_boolean_model.utils.Summable;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;

/**
 * Class representing a ranked subcontent.
 */
public abstract class RankedSubcontent
        implements Comparable<RankedSubcontent>, Summable<RankedSubcontent>/* TODO: rethink about this */, Serializable {
    /**
     * The ranked subcontent.
     */
    private final Pair<@NotNull ContentRank, @NotNull String> rankedSubcontent;

    /**
     * Constructor.
     *
     * @param rank       The rank for this subcontent.
     * @param subcontent The subcontent.
     */
    public RankedSubcontent(@NotNull ContentRank rank, @NotNull String subcontent) {
        this.rankedSubcontent = new Pair<>(Objects.requireNonNull(rank), Objects.requireNonNull(subcontent));
    }

    /**
     * @return the rank of this instance.
     */
    @NotNull
    public ContentRank getRank() {
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
        RankedSubcontent that = (RankedSubcontent) o;
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
     * @param rankedSubcontent The other instance used for the comparison.
     */
    @Override
    public int compareTo(@NotNull RankedSubcontent rankedSubcontent) {
        return this.getRank().compareTo(rankedSubcontent.getRank());
    }

    /**
     * Interface representing the rank for a content.
     */
    public interface ContentRank extends Comparable<ContentRank>, Serializable {
    }

}