package it.units.informationretrieval.ir_boolean_model.entities;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An instance of this class represents a document.
 *
 * @author Matteo Ferfoglia
 */
public abstract class Document implements Serializable, Comparable<Document> {

    /**
     * The language of this instance.
     */
    @NotNull
    private final Language language;
    /**
     * The title of the document.
     */
    @Nullable
    private String title;
    /**
     * The {@link List} in which elements are the normalized
     * tokens of the title.
     * This field can be used to give a higher rank to documents
     * which have in the title terms in common with a query (it
     * would mean that some query terms are present in the
     * title, and so the document is probably more relevant).
     */
    @Nullable
    private List<String> normalizedTokensComposingTitle;
    /**
     * The content of this instance.
     */
    @Nullable
    private DocumentContent content;

    /**
     * Constructor.
     *
     * @param title    The title of this instance.
     * @param content  The content of this instance.
     * @param language The language of this instance.
     */
    public Document(@NotNull String title, @NotNull DocumentContent content, @NotNull Language language) {
        this(language);
        this.title = Objects.requireNonNull(title, "The document title cannot be null.");
        this.content = Objects.requireNonNull(content, "The content cannot be null.");
    }

    /**
     * Constructor.
     *
     * @param language The language of this instance.
     */
    protected Document(@NotNull Language language) {
        this.language = Objects.requireNonNull(language);
    }

    /**
     * Getter for {@link #title}.
     *
     * @return The title of this instance.
     */
    @Nullable
    public String getTitle() {
        return title;
    }

    /**
     * Setter for {@link #title}.
     *
     * @param title The content.
     */
    protected void setTitle(@NotNull String title) {
        this.title = Objects.requireNonNull(title);
    }

    /**
     * Getter for {@link #content}.
     * This method can be overridden, in fact the content does not need
     * to be stored in RAM for all the time, but it can be stored anywhere
     * and this method should know how to retrieve it.
     *
     * @return The content of this instance.
     */
    @Nullable
    public DocumentContent getContent() {
        return content;
    }

    /**
     * Setter for {@link #content}.
     *
     * @param content The content.
     */
    protected void setContent(@NotNull final DocumentContent content) {
        this.content = Objects.requireNonNull(content);
    }

    @NotNull
    public String toString() {
        return "{\"" +
                (title != null ? Utility.encodeForJson(title) : "")
                + "\": " + toJson()
                + "}";
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
     * @return A {@link LinkedHashMap} (sorted) having as keys the names of
     * the properties of this instance that you want to expose and as values
     * the correspondent value of that attributes.
     */
    public @NotNull LinkedHashMap<String, ?> toSortedMapOfProperties() {
        return new LinkedHashMap<>() {{
            assert getContent() != null;
            put("Content", getContent().getEntireTextContent());
        }};
    }

    /**
     * @param stringList The string list to be searched in the title for the matching.
     *                   <strong>No normalization</strong> is performed on the input
     *                   parameters.
     * @return the number of (normalized) words in common between
     * the title of this instance and the input parameters of the method.
     */
    public long howManyCommonNormalizedWords(List<String> stringList) {
        if (title == null) {
            return 0;
        } else {
            if (normalizedTokensComposingTitle == null) {
                normalizedTokensComposingTitle = Arrays.stream(Utility.split(title))
                        .map(token -> Utility.normalize(token, true, language))
                        .filter(Objects::nonNull)
                        .toList();
            }
            return normalizedTokensComposingTitle.stream().filter(stringList::contains).count();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Document document = (Document) o;

        if (language != document.language) return false;
        if (!Objects.equals(title, document.title)) return false;
        return Objects.equals(content, document.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(language, title, content);
    }
}