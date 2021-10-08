package documentDescriptors;

import components.Corpus;
import components.Document;
import components.Posting;
import org.jetbrains.annotations.NotNull;
import util.FunctionThrowingException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Class representing a Movie.
 *
 * @author Matteo Ferfoglia
 */
public class Movie extends Document {

    // TODO : implement this class. Currently it is just a draft.

    /**
     * The title of the movie.
     */
    @NotNull
    private final String title;

    /**
     * The description of the movie.
     */
    @NotNull
    private final String description;

    /**
     * Constructor.
     *
     * @param movieTitle       The movie title.
     * @param movieDescription The movie description.
     */
    public Movie(@NotNull String movieTitle, @NotNull String movieDescription) {
        super();
        this.title = movieTitle;
        this.description = movieDescription;
        super.setContent(movieTitle + " " + movieDescription);   // TODO : inefficient because a new string which is the concatenation is created
    }

    /**
     * Create a {@link Corpus} of {@link Movie}s.
     *
     * @throws components.Posting.DocumentIdentifier.NoMoreDocIdsAvailable If no more {@link components.Posting.DocumentIdentifier}s can be generated.
     * @throws URISyntaxException                                          If an exception is thrown while getting the URI of the files containing the information.
     */
    @NotNull
    public static Corpus createCorpus()
            throws Posting.DocumentIdentifier.NoMoreDocIdsAvailable, URISyntaxException {
        // TODO : this method is just a draft

        // Path to find files
        String folderName = "movies_dataset",
                movieDescriptionFile = folderName + "/plot_summaries.txt",
                movieMetadataFile = folderName + "/movie.metadata.tsv";

        // Function to open a file, given its name (path to the file), and returns it as BufferedReader
        FunctionThrowingException<String, BufferedReader, URISyntaxException> openFile = filePath ->
                new BufferedReader(
                        new InputStreamReader(
                                Objects.requireNonNull(Movie.class.getClassLoader().getResourceAsStream(filePath), "Invalid null resource")
                        )
                );

        // Read movie names
        // saves movie id as key and corresponding title as value
        Map<Integer, Movie> movieNames =
                openFile.apply(movieMetadataFile)
                        .lines()
                        .parallel()
                        .map(aMovie -> {
                            String[] movieFields = aMovie.split("\t");
                            // TODO : improve: you can use also the other metadata of movies!
                            return new AbstractMap.SimpleEntry<>(Integer.parseInt(movieFields[0])/*id*/, movieFields[2]/*title*/);
                        })
                        .collect(Collectors.toConcurrentMap(
                                Map.Entry::getKey,
                                entry -> new Movie(entry.getValue(), "")
                                // DuplicateKeyException if two equal keys are found
                        ));

        // Read movie descriptions
        // saves movie id as key and corresponding description as value // TODO : code duplication here
        Map<Integer, Movie> movieDescriptions =
                openFile.apply(movieDescriptionFile)
                        .lines()
                        .parallel()
                        .map(aMovie -> {
                            String[] movieFields = aMovie.split("\t");
                            return new AbstractMap.SimpleEntry<>(Integer.parseInt(movieFields[0])/*id*/, movieFields[1]/*description*/);
                        })
                        .collect(Collectors.toConcurrentMap(
                                Map.Entry::getKey,
                                entry -> new Movie("", entry.getValue())
                                // DuplicateKeyException if two equal keys are found
                        ));

        // Merge movie names with corresponding descriptions
        // movie id as key, corresponding movie as value
        Map<Integer, Movie> movies = Stream.of(movieNames, movieDescriptions)
                .parallel()
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(
                        Collectors.toConcurrentMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (movie1, movie2) ->   // Merging function
                                        new Movie(
                                                movie1.title.isEmpty() ? movie2.title : movie1.title,
                                                movie1.description.isEmpty() ? movie2.description : movie1.description
                                        )
                        )
                );

        // Create the corpus
        return new Corpus(movies.values());

    }

    @Override
    public String toString() {
        return "Movie{" +
                "title='" + title + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}