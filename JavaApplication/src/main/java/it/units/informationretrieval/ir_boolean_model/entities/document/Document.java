package it.units.informationretrieval.ir_boolean_model.entities.document;

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
     * The content of this instance.
     */
    @Nullable
    private DocumentContent content;   // TODO: The content of the document does not need to be stored in RAM, but the system must know hot to retrieve it quickly. This may be the task of "getContent()

    /**
     * Constructor.
     *
     * @param content The content of the document.
     */
    public Document(@NotNull String title, @NotNull DocumentContent content) {
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
     * Setter for {@link #title}.
     *
     * @param title The content.
     */
    protected void setTitle(@NotNull String title) {
        this.title = Objects.requireNonNull(title);
    }

    @NotNull
    public String toString() {// TODO : test
        return title + "\t" + toJson();
    }

    /**
     * @return The JSON representation of this instance
     */
    @NotNull
    public String toJson() {    // TODO : test
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
    public abstract @NotNull LinkedHashMap<String, ?> toSortedMapOfProperties();    // TODO: test

}