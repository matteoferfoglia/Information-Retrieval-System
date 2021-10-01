package components;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/** An instance of this class represents a document.
 * @author Matteo Ferfoglia */
public abstract class Document {

    /** The content of this instance. */
    @NotNull
    private String content;   // TODO: The content of the document does not need to be stored in RAM, but the system must know hot to retrieve it quickly. This may be the task of "getContent()

    /** Constructor.
     * @param content The content of the document.*/
    public Document(@NotNull String content) {
        this.content = Objects.requireNonNull(content, "The content cannot be null.");
    }

    /** Constructor. */
    protected Document() {
        this("");
    }

    /** Setter for {@link #content}.
     * @param content The content.*/
    protected void setContent(@NotNull String content) {
        this.content = content;
    }

    /** Getter for {@link #content}.
     * @return The content of this instance. */
    @NotNull
    public String getContent() {
        return content;
    }
}
