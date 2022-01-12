package it.units.informationretrieval.ir_boolean_model.entities;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Class representing the content of a {@link Document}.
 * The content of a {@link Document} is <strong>anything</strong>
 * concerning the {@link Document}, including the title.
 * Different part of the document may have different ranks.
 * Ranking is not implemented by this class.
 *
 * @param content The list of {@link String}s present in this instance
 *                of {@link Document}. The list should be sorted according to the
 *                order in which the content actually appears in the document.
 */
public record DocumentContent(@NotNull List<String> content) implements Serializable {

    /**
     * Constructor.
     *
     * @param content The list of {@link String}s present in this instance
     *                of {@link Document}. The list should be sorted according to the
     *                order in which the content actually appears in the document.
     */
    public DocumentContent(@NotNull List<@NotNull String> content) {
        this.content = Objects.requireNonNull(content);
    }

    /**
     * @return The entire content of this instance.
     */
    @NotNull
    public String getEntireTextContent() {
        return content.stream().sequential()
                .collect(Collectors.joining(System.lineSeparator()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DocumentContent that = (DocumentContent) o;

        return Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return content.hashCode();
    }
}
