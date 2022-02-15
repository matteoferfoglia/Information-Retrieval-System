package it.units.informationretrieval.ir_boolean_model.user_defined_contents.movies;

import com.fasterxml.jackson.core.JsonProcessingException;
import it.units.informationretrieval.ir_boolean_model.entities.*;
import it.units.informationretrieval.ir_boolean_model.exceptions.NoMoreDocIdsAvailable;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import it.units.informationretrieval.ir_boolean_model.utils.functional.FunctionThrowingException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Class representing a Movie.
 *
 * @author Matteo Ferfoglia
 */
public class Movie extends Document implements Serializable {

    /**
     * Name of file in resources containing movie descriptions.
     */
    @NotNull
    private static final String MOVIE_DESCRIPTIONS_FILE_NAME = "plot_summaries.txt";
    /**
     * Name of file in resources containing movie metadata.
     */
    @NotNull
    private static final String MOVIE_METADATA_FILE_NAME = "movie.metadata.tsv";
    /**
     * The regex used to separate field of input metadata
     */
    @NotNull
    private static final String REGEX_MOVIE_METADATA_SEPARATOR = "\t";      // data taken from tsv files
    /**
     * The language of the corpus from which all movies are taken.
     * Notice: this field indicates the language of the corpus, NOT the
     * language of movies, which is read from the corpus and specified in
     * {@link #languageKeys}.
     */
    private static final Language MOVIE_CORPUS_LANGUAGE = Language.ENGLISH;
    /**
     * {@link ConcurrentMap} having as key an index value for the language and as
     * value its corresponding language.
     */
    @NotNull
    private static final ConcurrentMap<String, String> LANGUAGES_MAP = new ConcurrentHashMap<>();
    /**
     * {@link ConcurrentMap} having as key an index value for the movie production
     * country, and as value its corresponding country.
     */
    @NotNull
    private static final ConcurrentMap<String, String> COUNTRIES_MAP = new ConcurrentHashMap<>();
    /**
     * {@link ConcurrentMap} having as key an index value for the movie genre, and
     * as value its corresponding genre.
     */
    @NotNull
    private static final ConcurrentMap<String, String> GENRES_MAP = new ConcurrentHashMap<>();
    /**
     * The title of the movie.
     */
    @Nullable
    private String movieTitle;
    /**
     * The release date of the movie.
     */
    @Nullable
    private LocalDate releaseDate;
    /**
     * The Box office revenue of the movie, in dollars.
     */
    private long boxOfficeRevenue = -1; // initial value if not available
    /**
     * Running time of the movie, in seconds.
     */
    private int runTime = -1;       // initial value if not available
    /**
     * The {@link List} of keys to the languages of the movie, taken from {@link #LANGUAGES_MAP}.
     * A {@link List} is used because a movie may have more than a language.
     */
    @Nullable
    private List<String> languageKeys;
    /**
     * The {@link List} of keys to the production countries of the movie, taken from {@link #COUNTRIES_MAP}.
     * A {@link List} is used because a movie may have been produced in more than a country.
     */
    @Nullable
    private List<String> countryKeys;
    /**
     * The {@link List} of keys to the genres of the movie, taken from {@link #GENRES_MAP}.
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
        super(MOVIE_CORPUS_LANGUAGE);
        this.description = description;
    }

    /**
     * Constructor to initialize an instance with null values, for all fields
     * but the {@link #description} which will be set to the default value.
     * An array of metadata is expected as input parameter, and it must
     * contain the metadata in the correct order, as specified by {@link MOVIE_METADATA}.
     */
    private Movie(Object[] metadata) {
        super(MOVIE_CORPUS_LANGUAGE);

        this.movieTitle = (String) metadata[MOVIE_METADATA.MOVIE_NAME.getPositionInFile()];
        assert this.movieTitle != null;
        super.setTitle(movieTitle);
        this.releaseDate = parseData(((String) metadata[MOVIE_METADATA.MOVIE_RELEASE_DATE.getPositionInFile()]));

        // box office revenue conversion
        String boxOfficeRevenueAsString = (String) metadata[MOVIE_METADATA.MOVIE_BOX_OFFICE_REVENUE.getPositionInFile()];
        this.boxOfficeRevenue = boxOfficeRevenueAsString.strip().isEmpty() ? this.boxOfficeRevenue : Long.parseLong(boxOfficeRevenueAsString);

        // runtime conversion
        String runTimeAsString = ((String) metadata[MOVIE_METADATA.MOVIE_RUNTIME.getPositionInFile()]).strip();
        this.runTime = runTimeAsString.isEmpty() ? this.runTime : (int) (Double.parseDouble(runTimeAsString) * 60);   // convert to seconds

        BiFunction<String, ConcurrentMap<String, String>, List<String>> fromJsonToListWithParsing =
                (jsonString, concurrentMap) -> {
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
        this.languageKeys = fromJsonToListWithParsing.apply((String) metadata[MOVIE_METADATA.MOVIE_LANGUAGES.getPositionInFile()], LANGUAGES_MAP);
        this.countryKeys = fromJsonToListWithParsing.apply((String) metadata[MOVIE_METADATA.MOVIE_COUNTRIES.getPositionInFile()], COUNTRIES_MAP);
        this.genreKeys = fromJsonToListWithParsing.apply((String) metadata[MOVIE_METADATA.MOVIE_GENRES.getPositionInFile()], GENRES_MAP);
    }

    /**
     * Constructor.
     *
     * @param movieTitle       The movie title.
     * @param releaseDate      The movie release date.
     * @param boxOfficeRevenue The movie box office revenue in dollars.
     * @param runTime          The movie running time in seconds.
     * @param languageKeys     The keys to the movie languages in {@link #LANGUAGES_MAP}
     * @param countryKeys      The keys to the movie production country in {@link #COUNTRIES_MAP}
     * @param genreKeys        The keys to the movie genres in {@link #GENRES_MAP}
     * @param movieDescription The movie description.
     */
    public Movie(@NotNull String movieTitle, @Nullable LocalDate releaseDate,
                 long boxOfficeRevenue, int runTime,
                 @NotNull List<String> languageKeys, @NotNull List<String> countryKeys,
                 @NotNull List<String> genreKeys, @NotNull String movieDescription) {
        super(MOVIE_CORPUS_LANGUAGE);
        this.movieTitle = Objects.requireNonNull(movieTitle);
        this.releaseDate = releaseDate;
        this.boxOfficeRevenue = boxOfficeRevenue;
        this.runTime = runTime;
        this.languageKeys = Objects.requireNonNull(languageKeys);
        this.countryKeys = Objects.requireNonNull(countryKeys);
        this.genreKeys = Objects.requireNonNull(genreKeys);
        this.description = Objects.requireNonNull(movieDescription);

        final DocumentContent content;
        {
            List<String> documentContent = new ArrayList<>();
            documentContent.add(this.movieTitle);
            if (this.releaseDate != null) { // may be null
                documentContent.add(String.valueOf(this.releaseDate));
            }
            documentContent.add(String.valueOf(this.boxOfficeRevenue));
            documentContent.add(String.valueOf(this.runTime));
            documentContent.add(this.languageKeys.stream().map(LANGUAGES_MAP::get).sorted().collect(Collectors.joining()));
            documentContent.add(this.countryKeys.stream().map(COUNTRIES_MAP::get).sorted().collect(Collectors.joining()));
            documentContent.add(this.genreKeys.stream().map(GENRES_MAP::get).sorted().collect(Collectors.joining()));
            documentContent.add(this.description);
            content = new DocumentContent(documentContent);
        }

        super.setTitle(this.movieTitle);
        super.setContent(content);
    }

    /**
     * No-args constructor.
     */
    public Movie() {
        super(MOVIE_CORPUS_LANGUAGE);
    }

    /**
     * Parses and returns the data given as input.
     * This data parser is specific for the data format used in the movie corpus.
     *
     * @param dataAsString The data as {@link String}.
     * @return The {@link LocalDate} corresponding to the input parameter or null if invalid.
     */
    @Nullable
    private static LocalDate parseData(String dataAsString) {
        String[] yyyy_mm_dd = dataAsString.split("-");
        int dd = 1, mm = 1, yyyy;       // days in 1..28/31, months in 1..12, initialized to 1
        if (yyyy_mm_dd.length == 3) {
            dd = Integer.parseInt(yyyy_mm_dd[2]);
            mm = Integer.parseInt(yyyy_mm_dd[1]);
            yyyy = Integer.parseInt(yyyy_mm_dd[0]);
        } else if (yyyy_mm_dd.length == 2) {   // only the year and the month are provided
            // day set to 0
            mm = Integer.parseInt(yyyy_mm_dd[1]);
            yyyy = Integer.parseInt(yyyy_mm_dd[0]);
        } else if (yyyy_mm_dd.length == 1 && !yyyy_mm_dd[0].strip().isEmpty()) {   // only the year is provided
            // day and month set to 0
            yyyy = Integer.parseInt(yyyy_mm_dd[0]);
        } else {
            return null;
        }
        return LocalDate.of(yyyy, mm, dd);
    }

    /**
     * Create a {@link Corpus} of {@link Movie}s.
     *
     * @throws NoMoreDocIdsAvailable If no more {@link DocumentIdentifier}s can be generated.
     * @throws URISyntaxException    If an exception is thrown while getting the URI of the files containing the information.
     */
    @NotNull
    static Corpus createCorpus() throws NoMoreDocIdsAvailable, URISyntaxException {

        // Function to open a file, given its name (path to the file), and returns it as BufferedReader
        FunctionThrowingException<String, BufferedReader, URISyntaxException> openFileFunction = filePath ->
                new BufferedReader(new InputStreamReader(Objects.requireNonNull(
                        Movie.class.getResourceAsStream(filePath), "Invalid null resource")));

        ConcurrentMap<Integer, Movie> movieMetaData = getMovieMetadata(openFileFunction);
        ConcurrentMap<Integer, Movie> movieDescriptions = getMovieDescriptions(openFileFunction);
        ConcurrentMap<Integer, Movie> movies = mergeMovieMetadataWithDescriptionsAndGet(movieMetaData, movieDescriptions);

        // Create the corpus avoiding movies without the title
        return new Corpus(
                movies.values()
                        .stream().unordered().parallel()
                        .filter(aMovie -> aMovie.movieTitle != null)
                        .collect(Collectors.toList()),
                MOVIE_CORPUS_LANGUAGE);

    }

    /**
     * Merges movie metadata with corresponding descriptions.
     *
     * @param movieMetaData     the {@link ConcurrentMap} having movie ids as key and corresponding metadata as value.
     * @param movieDescriptions the {@link ConcurrentMap} having movie ids as key and corresponding description as value.
     * @return the {@link ConcurrentMap} having movie ids as key and corresponding {@link Movie} instance resulting
     * from merging metadata and descriptions as value.
     */
    @NotNull
    private static ConcurrentMap<Integer, Movie> mergeMovieMetadataWithDescriptionsAndGet(
            @NotNull final ConcurrentMap<Integer, Movie> movieMetaData,
            @NotNull final ConcurrentMap<Integer, Movie> movieDescriptions) {

        return Stream.of(movieMetaData, movieDescriptions)
                .unordered().parallel()
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(
                        Collectors.toConcurrentMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (movie1, movie2) -> {// Merging function
                                    boolean areMetadataInMovie2 = movie1.movieTitle == null;
                                    Movie movieWithMetadata = areMetadataInMovie2 ? movie2 : movie1;
                                    Movie movieWithDescription = areMetadataInMovie2 ? movie1 : movie2;
                                    return new Movie(
                                            Objects.requireNonNull(movieWithMetadata.movieTitle),
                                            movieWithMetadata.releaseDate,
                                            movieWithMetadata.boxOfficeRevenue,
                                            movieWithMetadata.runTime,
                                            Objects.requireNonNull(movieWithMetadata.languageKeys),
                                            Objects.requireNonNull(movieWithMetadata.countryKeys),
                                            Objects.requireNonNull(movieWithMetadata.genreKeys),
                                            Objects.requireNonNull(movieWithDescription.description)
                                    );
                                }));
    }

    /**
     * Reads movie descriptions.
     *
     * @param openFileFunction The {@link Function} ables to open the file containing the data
     *                         and to return the buffer with the data.
     * @return a {@link ConcurrentMap} having movie ids as key and corresponding {@link Movie} instance
     * (initialized with its metadata) as value. The returned {@link Movie} instance is initialized
     * with the description, while other metadata are set to null.
     */
    @NotNull
    private static ConcurrentMap<Integer, Movie> getMovieDescriptions(
            FunctionThrowingException<String, BufferedReader, URISyntaxException> openFileFunction)
            throws URISyntaxException {

        Function<String, Map.Entry<Integer, String>> getMovieDescriptionsIndexedByWikipediaMovieID = (aLineOfMovieDescriptionFile) -> {
            String[] movieFields = aLineOfMovieDescriptionFile.split(REGEX_MOVIE_METADATA_SEPARATOR);
            return new AbstractMap.SimpleEntry<>(Integer.parseInt(movieFields[0])/*id*/, movieFields[1]/*description*/);
        };
        return getMovieDataFromCorpus(
                openFileFunction.apply(MOVIE_DESCRIPTIONS_FILE_NAME),
                getMovieDescriptionsIndexedByWikipediaMovieID,
                Collectors.toConcurrentMap(
                        Map.Entry::getKey,
                        entry -> new Movie(entry.getValue())
                        /* DuplicateKeyException if two equal keys are found*/));
    }

    /**
     * Reads movie metadata.
     *
     * @param openFileFunction The {@link Function} ables to open the file containing the data
     *                         and to return the buffer with the data.
     * @return a {@link ConcurrentMap} having movie ids as key and corresponding movie instance
     * (initialized with its metadata) as value. The returned {@link Movie} instance is initialized
     * with the metadata, while the description is set to null.
     */
    @NotNull
    private static ConcurrentMap<Integer, Movie> getMovieMetadata(
            FunctionThrowingException<String, BufferedReader, URISyntaxException> openFileFunction)
            throws URISyntaxException {

        final int NUMBER_OF_METADATA = 9;
        Function<String, Map.Entry<Integer, String[]>> getMovieMetadataIndexedByWikipediaMovieID =
                (aLineOfMovieMetadataFromFile) -> {
                    String[] movieFields = aLineOfMovieMetadataFromFile.split(REGEX_MOVIE_METADATA_SEPARATOR);
                    return new AbstractMap.SimpleEntry<>(
                            Integer.parseInt(movieFields[0])/*id*/,
                            IntStream.range(0, NUMBER_OF_METADATA)
                                    .sequential()
                                    .mapToObj(i -> movieFields[i])
                                    .toArray(String[]::new) /*array of metadata*/);
                };
        return getMovieDataFromCorpus(
                openFileFunction.apply(MOVIE_METADATA_FILE_NAME),
                getMovieMetadataIndexedByWikipediaMovieID,
                Collectors.toConcurrentMap(
                        Map.Entry::getKey,
                        (Map.Entry<Integer, String[]> entry) -> new Movie(entry.getValue())
                        /* DuplicateKeyException if two equal keys are found*/));
    }

    /**
     * This method extracts data about movies from a buffer.
     *
     * @param bufferWithData  The {@link BufferedReader} containing the data to extract.
     * @param movieDataMapper The {@link Function} which maps each line of the buffer
     *                        to a {@link Map.Entry}. Each {@link Map.Entry} has the movie identifier as index
     *                        and the data as value.
     * @param <T>             The type of values of the {@link Map.Entry} returned by the mapper function.
     * @param collector       The {@link Collector} to collect all entries from the mapper function.
     * @return The {@link ConcurrentMap} having as key the movie identifier and as corresponding value the
     * {@link Movie} instance, obtained from the mapper function.
     */
    @NotNull
    private static <T> ConcurrentMap<Integer, Movie> getMovieDataFromCorpus(
            @NotNull final BufferedReader bufferWithData,
            @NotNull final Function<String, Map.Entry<Integer, T>> movieDataMapper,
            Collector<Map.Entry<Integer, T>, ?, ConcurrentMap<Integer, Movie>> collector) {

        return bufferWithData
                .lines().unordered().parallel()
                .map(movieDataMapper)
                .collect(collector);
    }

    @Override
    public @NotNull LinkedHashMap<String, ?> toSortedMapOfProperties() {
        assert movieTitle != null;
        assert languageKeys != null;
        assert countryKeys != null;
        assert genreKeys != null;

        BiFunction<@NotNull List<@NotNull String>, Map<@NotNull String, @NotNull String>, @NotNull List<?>> keyListToValueList =
                (keyList, mapKeyToValue) ->
                        keyList.stream()
                                .map(mapKeyToValue::get)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toList());

        Function<@NotNull LocalDate, @NotNull String> dateToString = date -> date.atStartOfDay() + ":00Z";

        LinkedHashMap<String, Object> mapOfProperties = new LinkedHashMap<>();

        mapOfProperties.put("Title", Utility.encodeForJson(movieTitle));
        mapOfProperties.put("Release date", releaseDate != null ? Utility.encodeForJson(dateToString.apply(releaseDate)) : null);
        mapOfProperties.put("Box office revenue", boxOfficeRevenue > 0 ? Utility.encodeForJson(boxOfficeRevenue + " $") : null);
        mapOfProperties.put("Run time", runTime > 0 ? Utility.encodeForJson(runTime / 60 + " min") : null);
        mapOfProperties.put("Languages", keyListToValueList.apply(languageKeys, LANGUAGES_MAP));
        mapOfProperties.put("Countries", keyListToValueList.apply(countryKeys, COUNTRIES_MAP));
        mapOfProperties.put("Genres", keyListToValueList.apply(genreKeys, GENRES_MAP));
        mapOfProperties.put("Description", description == null ? null : Utility.encodeForJson(description));

        return mapOfProperties;
    }

    /**
     * Compare movies according to their title.
     */
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
     * Enum all metadata which are present in the corpus of movies,
     * saving the position in which they appear in the input file.
     */
    private enum MOVIE_METADATA {
        WIKIPEDIA_MOVIE_ID(0),
        FREEBASE_MOVIE_ID(1),
        MOVIE_NAME(2),
        MOVIE_RELEASE_DATE(3),
        MOVIE_BOX_OFFICE_REVENUE(4),
        MOVIE_RUNTIME(5),
        MOVIE_LANGUAGES(6),
        MOVIE_COUNTRIES(7),
        MOVIE_GENRES(8);

        /**
         * The position of the metadata in the input file.
         */
        private final int POSITION;

        /**
         * @param position The position of the metadata in the input file.
         */
        MOVIE_METADATA(int position) {
            POSITION = position;
        }

        /**
         * @return The position of the metadata in the input file.
         */
        public int getPositionInFile() {
            return POSITION;
        }
    }

}