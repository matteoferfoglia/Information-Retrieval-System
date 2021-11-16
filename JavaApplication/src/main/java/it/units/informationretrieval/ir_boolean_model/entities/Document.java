package it.units.informationretrieval.ir_boolean_model.entities;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.units.informationretrieval.ir_boolean_model.utils.Pair;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import it.units.informationretrieval.ir_boolean_model.utils.Properties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * An instance of this class represents a document.
 *
 * @author Matteo Ferfoglia
 */
public abstract class Document implements Serializable, Comparable<Document> {

    @Nullable
    private String title;

    /**
     * The content of this instance.
     */
    @Nullable
    private Content content;   // TODO: The content of the document does not need to be stored in RAM, but the system must know hot to retrieve it quickly. This may be the task of "getContent()

    /**
     * Constructor.
     *
     * @param content The content of the document.
     */
    public Document(@NotNull String title, @NotNull Content content) {
        this.title = Objects.requireNonNull(title, "The document title cannot be null.");
        this.content = Objects.requireNonNull(content, "The content cannot be null.");
    }

    /**
     * Constructor.
     */
    protected Document() {
    }

    /**
     * Getter for {@link #content}.
     *
     * @return The content of this instance.
     */
    @Nullable
    public Content getContent() {
        return content;
    }

    /**
     * Setter for {@link #content}.
     *
     * @param content The content.
     */
    protected void setContent(@NotNull final Content content) {
        this.content = Objects.requireNonNull(content);
    }

    /**
     * Setter for {@link #title}.
     *
     * @param title The content.
     */
    protected void setTitle(@NotNull String title) {
        this.title = Objects.requireNonNull(title);
    }

    @NotNull
    public String toString() {
        return title + "\t" + toJson();
    }

    /**
     * @return The JSON representation of this instance
     */
    @NotNull
    public String toJson() {
        LinkedHashMap<?, ?> mapOfProperties = toSortedMapOfProperties();
        try {
            return Utility.convertToJson(mapOfProperties);
        } catch (JsonProcessingException e) {
            Logger.getLogger(this.getClass().getCanonicalName())
                    .log(Level.WARNING, "Error during JSON serialization of " + mapOfProperties + ".", e);
            return "{}";
        }
    }

    /**
     * @return A {@link LinkedHashMap} (sortedd) having as keys the names of
     * the properties of this instance that you want to expose and as values
     * the correspondent value of that attributes.
     */
    public abstract @NotNull LinkedHashMap<String, ?> toSortedMapOfProperties();


    /**
     * Class representing the content of a {@link Document}.
     * Different part of the document may have different ranks.
     */
    public static class Content implements Serializable {

        /**
         * {@link List} of {@link RankedSubcontent}.
         * The list is sorted according to the order in which the subcontent appears
         * in the document.
         */
        private final List<RankedSubcontent> content;

        /**
         * Constructor.
         *
         * @param rankedSubcontentList The list of {@link RankedSubcontent}s present in
         *                             this instance of {@link Document}. The list should be sorted according to the
         *                             order in which it appears in the document.
         */
        public Content(@NotNull List<@NotNull RankedSubcontent> rankedSubcontentList) {
            this.content = Objects.requireNonNull(rankedSubcontentList);
        }

        /**
         * @return The entire content of this instance.
         */
        @NotNull
        public String getEntireTextContent() {
            return content.stream().sequential()
                    .map(RankedSubcontent::getContent)
                    .collect(Collectors.joining("\n"));
        }

        public interface Summable<T> {
            /**
             * @param t The other instance
             * @return the sum of this instance with the other.
             */
            int sum(@NotNull T t);

            /**
             * @param tCollection The collection of other instances to be sum to this one.
             * @return the sum of this instance with the sum of all the other.
             */
            int sum(@NotNull Collection<@NotNull T> tCollection);
        }

        /**
         * Class representing a ranked subcontent.
         */
        public abstract static class RankedSubcontent implements Comparable<RankedSubcontent>, Summable<RankedSubcontent>, Serializable {
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
             * Compare this instance with the given one according to {@link ContentRank#compareTo(RankedSubcontent)}.
             *
             * @param rankedSubcontent The other instance used for the comparison.
             */
            @SuppressWarnings("JavaDoc")    // Warning: Javadoc pointing to itself
            @Override
            public int compareTo(@NotNull Document.Content.RankedSubcontent rankedSubcontent) {
                return this.getRank().compareTo(rankedSubcontent.getRank());
            }

            /**
             * The rank for a part of a {@link Content}.
             */
            public interface ContentRank extends Comparable<ContentRank>, Serializable {

            }

        }

    }
}
