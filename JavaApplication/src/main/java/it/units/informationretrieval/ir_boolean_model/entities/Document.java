package it.units.informationretrieval.ir_boolean_model.entities;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.LinkedHashMap;
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
     * The title of the document.
     */
    @Nullable
    private String title;
    /**
     * The content of this instance (including the title).
     * The content is anything concerning the document.
     */
    @Nullable
    private DocumentContent content;

    /**
     * Constructor.
     *
     * @param title   The title of this instance.
     * @param content The content of this instance (including the title).
     */
    public Document(@NotNull String title, @NotNull DocumentContent content) {
        this.title = Objects.requireNonNull(title, "The document title cannot be null.");
        this.content = Objects.requireNonNull(content, "The content cannot be null.");
    }

    /**
     * No-args constructor.
     */
    protected Document() {
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
     * @param title The title.
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

    /**
     * Getter for {@link #content}.
     *
     * @return The content of this instance as {@link String} or
     * an empty string if the content is null.
     */
    @NotNull
    public String getContentAsString() {
        return content == null ? "" : content.getEntireTextContent();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Document document = (Document) o;

        if (!Objects.equals(title, document.title)) return false;
        return Objects.equals(content, document.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, content);
    }
}