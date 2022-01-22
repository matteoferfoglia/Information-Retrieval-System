package it.units.informationretrieval.ir_boolean_model.user_defined_contents.cranfield;

import it.units.informationretrieval.ir_boolean_model.entities.Corpus;
import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;
import it.units.informationretrieval.ir_boolean_model.factories.CorpusFactory;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Class creating the {@link Corpus} of {@link CranfieldDocument}s.
 *
 * @author Matteo Ferfoglia
 */
public class CranfieldCorpusFactory extends CorpusFactory<CranfieldDocument> {

    /**
     * No-args constructor, required by the specifics of {@link CorpusFactory}.
     */
    public CranfieldCorpusFactory() {
        super(CranfieldDocument.class, "Cranfield collections");
    }

    @Override
    public Corpus createCorpus() throws NoMoreDocIdsAvailable, IOException {
        try {
            return CranfieldDocument.createCorpus();
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }
}
