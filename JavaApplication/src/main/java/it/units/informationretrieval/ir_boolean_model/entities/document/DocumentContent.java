package it.units.informationretrieval.ir_boolean_model.entities.document;

import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Class representing the content of a {@link Document}.
 * Different part of the document may have different ranks.
 */
public class DocumentContent implements Serializable {

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
    public DocumentContent(@NotNull List<@NotNull RankedSubcontent> rankedSubcontentList) {
        this.content = Objects.requireNonNull(rankedSubcontentList);
    }

    /**
     * @return The entire content of this instance.
     */
    @NotNull
    public String getEntireTextContent() {// TODO: test
        return content.stream().sequential()
                .map(RankedSubcontent::getContent)
                .collect(Collectors.joining(System.lineSeparator()));
    }

}
