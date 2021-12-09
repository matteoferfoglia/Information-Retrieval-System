package it.units.informationretrieval.ir_boolean_model.entities.fake_documents_descriptors;

import it.units.informationretrieval.ir_boolean_model.entities.Corpus;
import it.units.informationretrieval.ir_boolean_model.entities.Document;
import it.units.informationretrieval.ir_boolean_model.entities.DocumentIdentifier;
import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;

import java.util.Collection;
import java.util.Map;

public class FakeCorpus extends Corpus {
    /**
     * Constructor. Creates a corpus from a {@link Map}.
     *
     * @param corpusAsMap The corpus as input parameter.
     */
    public FakeCorpus(Map<DocumentIdentifier, Document> corpusAsMap) {
        super();
        var corpus = super.getCorpus();
        corpus.putAll(corpusAsMap);
    }

    public FakeCorpus(Collection<Document> documents) throws NoMoreDocIdsAvailable {
        super(documents);
    }

    public FakeCorpus() {
    }
}
