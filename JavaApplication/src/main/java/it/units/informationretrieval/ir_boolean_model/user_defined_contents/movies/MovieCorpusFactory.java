package it.units.informationretrieval.ir_boolean_model.user_defined_contents.movies;

import it.units.informationretrieval.ir_boolean_model.entities.Corpus;
import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;
import it.units.informationretrieval.ir_boolean_model.factories.CorpusFactory;

import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Class creating the {@link Corpus} of {@link Movie}s.
 *
 * @author Matteo Ferfoglia
 */
public class MovieCorpusFactory extends CorpusFactory<Movie> {

    /**
     * No-args constructor, required by the specifics of {@link CorpusFactory}.
     */
    public MovieCorpusFactory() {
        super(Movie.class, "Movie corpus");
    }

    @Override
    public Corpus createCorpus() throws NoMoreDocIdsAvailable, IOException {
        try {
            return Movie.createCorpus();
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }
    }
}
