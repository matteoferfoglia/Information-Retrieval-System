package it.units.informationretrieval.ir_boolean_model.document_descriptors;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.units.informationretrieval.ir_boolean_model.entities.*;
import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;
import it.units.informationretrieval.ir_boolean_model.utils.FunctionThrowingException;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Class representing a Movie.
 *
 * @author Matteo Ferfoglia
 */
public class Movie extends Document implements Serializable {

    // TODO : re-see this very very long class

    /**
     * Map having as key an index value for the language and as
     * value its corresponding language.
     */
    @NotNull
    private static final ConcurrentMap<String, String> languages = new ConcurrentHashMap<>();
    /**
     * Map having as key an index value for the movie production
     * country, and as value its corresponding country.
     */
    @NotNull
    private static final ConcurrentMap<String, String> countries = new ConcurrentHashMap<>();
    /**
     * Map having as key an index value for the movie genre, and
     * as value its corresponding genre.
     */
    @NotNull
    private static final ConcurrentMap<String, String> genres = new ConcurrentHashMap<>();
    /**
     * The title of the movie.
     */
    @Nullable
    private String movieTitle;
    /**
     * The released date of the movie.
     */
    @Nullable
    private LocalDate releaseDate;
    /**
     * The Box office revenue of the movie, in dollars.
     */
    private long boxOfficeRevenue = -1;
    /**
     * Running time of the movie, in seconds.
     */
    private int runningTime = -1;

    /**
     * The {@link List} of keys to the languages of the movie, taken from {@link #languages}.
     * A {@link List} is used because a movie may have more than a language.
     */
    @Nullable
    private List<String> languageKeys;
    /**
     * The {@link List} of keys to the production countries of the movie, taken from {@link #countries}.
     * A {@link List} is used because a movie may have been produced in more than a country.
     */
    @Nullable
    private List<String> countryKeys;
    /**
     * The {@link List} of keys to the genres of the movie, taken from {@link #genres}.
     * A {@link List} is used because a movie may be related to more than a genre.
     */
    @Nullable
    private List<String> genreKeys;

    /**
     * The description of the movie.
     */
    @Nullable
    private String description;

    /**
     * Constructor to initialize an instance with only the description and
     * all other fields will be set to the default value.
     */
    private Movie(@Nullable final String description) {
        this.description = description;
    }

    /**
     * Constructor to initialize an instance with nullable values, for all
     * fields but the {@link #description} which will be set to the default value.
     * An array of metadata is expected as input parameter and it must
     * contain the metadata in the correct order.
     */
    private Movie(Object[] metadata) {
        super();
        this.movieTitle = (String) metadata[0];
        assert this.movieTitle != null;
        super.setTitle(movieTitle);
        {
            // date time conversion
            String providedString = ((String) metadata[1]);
            String[] yyyy_mm_dd = providedString.split("-");
            try {
                int dd = 1, mm = 1, yyyy;       // days in 1..28/31, months in 1..12, initialized to 1
                if (yyyy_mm_dd.length == 3) {
                    dd = Integer.parseInt(yyyy_mm_dd[2]);
                    mm = Integer.parseInt(yyyy_mm_dd[1]);
                    yyyy = Integer.parseInt(yyyy_mm_dd[0]);
                } else if (yyyy_mm_dd.length == 2) {   // only the year and the month are provided
                    // day set to 0
                    mm = Integer.parseInt(yyyy_mm_dd[1]);
                    yyyy = Integer.parseInt(yyyy_mm_dd[0]);
                } else if (yyyy_mm_dd.length == 1 && !yyyy_mm_dd[0].trim().isEmpty()) {   // only the year is provided
                    // day and month set to 0
                    yyyy = Integer.parseInt(yyyy_mm_dd[0]);
                } else {
                    throw new DateTimeException("No release date provided for " + this.movieTitle);
                }
                this.releaseDate = LocalDate.of(yyyy, mm, dd);
            } catch (DateTimeException e) {
                this.releaseDate = null;
//                Logger.getLogger(this.getClass().getCanonicalName())
//                        .log(Level.WARNING, "Error with the release date of movie \"" + this.title + "\". " +
//                                "Provided releaseDate: " + providedString + " but invalid. " +
//                                "The releaseDate will be set to null.", e);
            }
        }

        {
            // box office revenue conversion
            String boxOfficeRevenueAsString = (String) metadata[2];
            this.boxOfficeRevenue = boxOfficeRevenueAsString.trim().isEmpty() ? 0 : Long.parseLong(boxOfficeRevenueAsString);
        }

        {
            // running time conversion
            String runningTimeAsString = (String) metadata[3];
            this.boxOfficeRevenue = runningTimeAsString.trim().isEmpty() ? 0 : (int) (Double.parseDouble((String) metadata[3]) * 60);   // convert to seconds
        }

        BiFunction<String, ConcurrentMap<String, String>, List<String>> fromJsonToListWithParsing = (jsonString, concurrentMap) -> {
            try {
                return Utility.convertFromJsonToMap(jsonString).entrySet().stream().unordered().parallel()
                        .map(entry -> {
                            String key = entry.getKey();
                            String value = (String) entry.getValue();
                            concurrentMap.putIfAbsent(key, value);
                            return key;
                        })
                        .collect(Collectors.toList());
            } catch (JsonProcessingException e) {
                Logger.getLogger(this.getClass().getCanonicalName())
                        .log(Level.WARNING, "Error during JSON deserialization of " + jsonString + "." +
                                " It will be ignored.", e);
                return new ArrayList<>(0);
            }
        };
        this.languageKeys = fromJsonToListWithParsing.apply((String) metadata[4], languages);
        this.countryKeys = fromJsonToListWithParsing.apply((String) metadata[5], countries);
        this.genreKeys = fromJsonToListWithParsing.apply((String) metadata[6], genres);
    }

    /**
     * Constructor.
     *
     * @param movieTitle       The movie title.
     * @param releaseDate      The movie release date.
     * @param boxOfficeRevenue The movie box office revenue in dollars.
     * @param runningTime      The movie running time in seconds.
     * @param languageKeys     The keys to the movie languages in {@link #languages}
     * @param countryKeys      The keys to the movie production country in {@link #countries}
     * @param genreKeys        The keys to the movie genres in {@link #genres}
     * @param movieDescription The movie description.
     */
    public Movie(@NotNull String movieTitle, @Nullable LocalDate releaseDate,
                 long boxOfficeRevenue, int runningTime,
                 @NotNull List<String> languageKeys, @NotNull List<String> countryKeys,
                 @NotNull List<String> genreKeys, @NotNull String movieDescription) {
        super();
        this.movieTitle = Objects.requireNonNull(movieTitle);
        this.releaseDate = releaseDate;
        this.boxOfficeRevenue = boxOfficeRevenue;
        this.runningTime = runningTime;
        this.languageKeys = Objects.requireNonNull(languageKeys);
        this.countryKeys = Objects.requireNonNull(countryKeys);
        this.genreKeys = Objects.requireNonNull(genreKeys);
        this.description = Objects.requireNonNull(movieDescription);

        final DocumentContent content;
        {
            // Creation of the ranked subcontents
            List<DocumentRankedSubcontent> documentRankedSubcontents = new ArrayList<>();
            documentRankedSubcontents.add(new MovieContentRank.MovieRankedSubcontent(new MovieContentRank(MovieContentRank.Rank.TITLE), this.movieTitle));
            if (this.releaseDate != null) { // may be null
                documentRankedSubcontents.add(new MovieContentRank.MovieRankedSubcontent(new MovieContentRank(MovieContentRank.Rank.RELEASE_DATE), String.valueOf(this.releaseDate)));
            }
            documentRankedSubcontents.add(new MovieContentRank.MovieRankedSubcontent(new MovieContentRank(MovieContentRank.Rank.BOX_OFFICE_REVENUE), String.valueOf(this.boxOfficeRevenue)));
            documentRankedSubcontents.add(new MovieContentRank.MovieRankedSubcontent(new MovieContentRank(MovieContentRank.Rank.RUNNING_TIME), String.valueOf(this.runningTime)));
            documentRankedSubcontents.add(new MovieContentRank.MovieRankedSubcontent(new MovieContentRank(MovieContentRank.Rank.LANGUAGE), this.languageKeys.stream().map(languages::get).collect(Collectors.joining())));
            documentRankedSubcontents.add(new MovieContentRank.MovieRankedSubcontent(new MovieContentRank(MovieContentRank.Rank.COUNTRY), this.countryKeys.stream().map(countries::get).collect(Collectors.joining())));
            documentRankedSubcontents.add(new MovieContentRank.MovieRankedSubcontent(new MovieContentRank(MovieContentRank.Rank.GENRE), this.genreKeys.stream().map(genres::get).collect(Collectors.joining())));
            documentRankedSubcontents.add(new MovieContentRank.MovieRankedSubcontent(new MovieContentRank(MovieContentRank.Rank.DESCRIPTION), this.description));
            content = new DocumentContent(documentRankedSubcontents);
        }

        super.setTitle(this.movieTitle);
        super.setContent(content);   // TODO : inefficient because a new string which is the concatenation is created
    }

    public Movie() {
    }

    /**
     * Create a {@link Corpus} of {@link Movie}s.
     *
     * @throws NoMoreDocIdsAvailable If no more {@link DocumentIdentifier}s can be generated.
     * @throws URISyntaxException    If an exception is thrown while getting the URI of the files containing the information.
     */
    @NotNull
    public static Corpus createCorpus() // TODO: try to generalize (e.g., an interface?)
            throws NoMoreDocIdsAvailable, URISyntaxException {
        // TODO : this method is just a draft

        // Path to find files
        String folderName = "movies_dataset",
                movieDescriptionFile = folderName + "/plot_summaries.txt",
                movieMetadataFile = folderName + "/movie.metadata.tsv";

        // Function to open a file, given its name (path to the file), and returns it as BufferedReader
        FunctionThrowingException<String, BufferedReader, URISyntaxException> openFile = filePath ->
                new BufferedReader(
                        new InputStreamReader(
                                Objects.requireNonNull(Movie.class.getResourceAsStream(filePath), "Invalid null resource")
                        )
                );

        // Read movie names
        // saves movie id as key and corresponding title as value
        Map<Integer, Movie> movieNames =
                openFile.apply(movieMetadataFile)
                        .lines().unordered().parallel()
                        .map(aMovie -> {
                            String[] movieFields = aMovie.split("\t");
                            return new AbstractMap.SimpleEntry<>(
                                    Integer.parseInt(movieFields[0])/*id*/,
                                    IntStream.range(2, 9).sequential().mapToObj(i -> movieFields[i]).toArray() /*array of metadata*/
                            );
                        })
                        .collect(Collectors.toConcurrentMap(
                                Map.Entry::getKey,
                                entry -> new Movie(entry.getValue())
                                // DuplicateKeyException if two equal keys are found
                        ));

        // Read movie descriptions
        // saves movie id as key and corresponding description as value // TODO : code duplication here
        Map<Integer, Movie> movieDescriptions =
                openFile.apply(movieDescriptionFile)
                        .lines()
                        .unordered().parallel()
                        .map(aMovie -> {
                            String[] movieFields = aMovie.split("\t");
                            return new AbstractMap.SimpleEntry<>(Integer.parseInt(movieFields[0])/*id*/, movieFields[1]/*description*/);
                        })
                        .collect(Collectors.toConcurrentMap(
                                Map.Entry::getKey,
                                entry -> new Movie(entry.getValue())
                                // DuplicateKeyException if two equal keys are found
                        ));

        // Merge movie names with corresponding descriptions
        // movie id as key, corresponding movie as value
        Map<Integer, Movie> movies = Stream.of(movieNames, movieDescriptions)
                .unordered().parallel()
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(
                        Collectors.toConcurrentMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (movie1, movie2) -> {// Merging function
                                    boolean areMetadataInMovie2 = movie1.movieTitle == null;
                                    Movie movieWithMetadati = areMetadataInMovie2 ? movie2 : movie1;
                                    Movie movieWithDescription = areMetadataInMovie2 ? movie1 : movie2;
                                    return new Movie(
                                            Objects.requireNonNull(movieWithMetadati.movieTitle),
                                            movieWithMetadati.releaseDate,
                                            movieWithMetadati.boxOfficeRevenue,
                                            movieWithMetadati.runningTime,
                                            Objects.requireNonNull(movieWithMetadati.languageKeys),
                                            Objects.requireNonNull(movieWithMetadati.countryKeys),
                                            Objects.requireNonNull(movieWithMetadati.genreKeys),
                                            Objects.requireNonNull(movieWithDescription.description)
                                    );
                                }
                        )
                );

        // Create the corpus avoiding movies without the title
        return new Corpus(
                movies.values()
                        .stream().unordered().parallel()
                        .filter(aMovie -> aMovie.movieTitle != null)
                        .collect(Collectors.toList())
        );

    }

    @Override
    public @NotNull LinkedHashMap<String, ?> toSortedMapOfProperties() {
        assert movieTitle != null;
        assert languageKeys != null;
        assert countryKeys != null;
        assert genreKeys != null;

        Utility.MyBiFunction<@NotNull List<@NotNull String>, Map<@NotNull String, @NotNull String>, @NotNull List<?>> keyListToValueList =
                (keyList, mapKeyToValue) ->
                        keyList.stream()
                                .map(mapKeyToValue::get)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());

        Function<@NotNull LocalDate, @NotNull String> dateToString = date -> date.atStartOfDay() + ":00Z";

        LinkedHashMap<String, Object> mapOfProperties = new LinkedHashMap<>();

        mapOfProperties.put("Title", movieTitle.replaceAll("\"", "'"));
        mapOfProperties.put("Release date", releaseDate != null ? dateToString.apply(releaseDate) : null);
        mapOfProperties.put("Box office revenue", boxOfficeRevenue > 0 ? boxOfficeRevenue + " $" : null);
        mapOfProperties.put("Running time", runningTime > 0 ? runningTime / 60 + " min" : null);
        mapOfProperties.put("Language", keyListToValueList.apply(languageKeys, languages));
        mapOfProperties.put("Country", keyListToValueList.apply(genreKeys, genres));
        mapOfProperties.put("Genre", keyListToValueList.apply(genreKeys, genres));
        mapOfProperties.put("Description", description);

        return mapOfProperties;
    }

    @Override
    public int compareTo(@NotNull Document otherDocument) {

        if (!(Objects.requireNonNull(otherDocument) instanceof Movie)) {
            throw new IllegalArgumentException("Incompatible type: the argument must be an instance of class " +
                    this.getClass().getCanonicalName());
        }

        Movie otherMovie = (Movie) otherDocument;

        if (movieTitle == null && otherMovie.movieTitle == null) {
            return 0;
        }
        if (movieTitle == null) {
            return -1;
        }
        if (otherMovie.movieTitle == null) {
            return +1;
        }
        return movieTitle.compareTo(otherMovie.movieTitle);
    }

    /**
     * Class implementing {@link DocumentContentRank} for {@link Movie}s.
     */
    private static class MovieContentRank implements DocumentContentRank {
        /**
         * The rank.
         */
        private final Rank rank;

        MovieContentRank(@NotNull final Rank rank) {
            this.rank = Objects.requireNonNull(rank);
        }

        @Override
        public int compareTo(@NotNull DocumentContentRank otherContentRank) {
            if (Objects.requireNonNull(otherContentRank) instanceof MovieContentRank) {
                return ((MovieContentRank) otherContentRank).rank.getRankValue() - this.rank.getRankValue();
            } else {
                throw new IllegalArgumentException(otherContentRank + " is not an instance of " +
                        this.getClass().getCanonicalName() + ", hence it is not comparable with " +
                        "this instance (" + this + ").");
            }
        }

        /**
         * Enum of possible ranks.
         */
        public enum Rank {
            TITLE(0),
            DESCRIPTION(1),
            GENRE(2),
            COUNTRY(3),
            LANGUAGE(4),
            RELEASE_DATE(5),
            RUNNING_TIME(6),
            BOX_OFFICE_REVENUE(7);
            /**
             * The numeric value for the rank. The <strong>lower</strong> the best.
             */
            private final int rankValue;

            /**
             * Constructor.
             *
             * @param rankValue The rank value: the lower the best.
             */
            Rank(int rankValue) {
                this.rankValue = rankValue;
            }

            public int getRankValue() {
                return rankValue;
            }
        }


        /**
         * Concrete cass extending {@link DocumentRankedSubcontent}.
         */
        static class MovieRankedSubcontent extends DocumentRankedSubcontent {

            /**
             * Constructor.
             *
             * @param rank       The rank for this subcontent.
             * @param subcontent The subcontent.
             */
            public MovieRankedSubcontent(@NotNull DocumentContentRank rank, @NotNull String subcontent) {
                super(Objects.requireNonNull(rank), Objects.requireNonNull(subcontent));
            }

            private static int sumRanks(@NotNull DocumentRankedSubcontent first, @NotNull DocumentRankedSubcontent second) {
                return sumRanks(
                        ((MovieContentRank) first.getRank()).rank.getRankValue(),
                        ((MovieContentRank) second.getRank()).rank.getRankValue()
                );
            }

            private static int sumRanks(int rank1, int rank2) {
                return (Rank.values().length - rank1) + (Rank.values().length - rank2);
            }

            @Override
            public int sum(@NotNull DocumentRankedSubcontent documentRankedSubcontent) {
                return sumRanks(this, documentRankedSubcontent);
            }

            @Override
            public int sum(@NotNull Collection<DocumentRankedSubcontent> documentRankedSubcontents) {
                return Objects.requireNonNull(documentRankedSubcontents)
                        .stream().unordered()
                        .map(aRank -> ((MovieContentRank) aRank.getRank()).rank.getRankValue())
                        .reduce(MovieRankedSubcontent::sumRanks)
                        .orElse(0);
            }
        }
    }
}