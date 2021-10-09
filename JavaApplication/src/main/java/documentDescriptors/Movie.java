package documentDescriptors;

import com.fasterxml.jackson.core.JsonProcessingException;
import components.Corpus;
import components.Document;
import components.Posting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.FunctionThrowingException;
import util.Utility;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
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
public class Movie extends Document implements Externalizable {

    // TODO : implement this class. Currently it is just a draft.

    /**
     * Map having as key an index value for the language and as
     * value its corresponding language.
     */
    @NotNull
    private static ConcurrentMap<String, String> languages = new ConcurrentHashMap<>();
    /**
     * Map having as key an index value for the movie production
     * country, and as value its corresponding country.
     */
    @NotNull
    private static ConcurrentMap<String, String> countries = new ConcurrentHashMap<>();
    /**
     * Map having as key an index value for the movie genre, and
     * as value its corresponding genre.
     */
    @NotNull
    private static ConcurrentMap<String, String> genres = new ConcurrentHashMap<>();
    /**
     * The title of the movie.
     */
    @Nullable
    private String title;
    /**
     * The released date of the movie.
     */
    @Nullable
    private LocalDate releaseDate;
    /**
     * The Box office revenue of the movie, in dollars.
     */
    private long boxOfficeRevenue;
    /**
     * Running time of the movie, in seconds.
     */
    private int runningTime;

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
        this.title = (String) metadata[0];
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
                    throw new DateTimeException("No release date provided for " + this.title);
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
                            Movie.languages.putIfAbsent(key, value);
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
        this.title = Objects.requireNonNull(movieTitle);
        this.releaseDate = releaseDate;
        this.boxOfficeRevenue = boxOfficeRevenue;
        this.runningTime = runningTime;
        this.languageKeys = Objects.requireNonNull(languageKeys);
        this.countryKeys = Objects.requireNonNull(countryKeys);
        this.genreKeys = Objects.requireNonNull(genreKeys);
        this.description = Objects.requireNonNull(movieDescription);

        final Content content;
        {
            // Creation of the ranked subcontents
            List<Content.RankedSubcontent> rankedSubcontents = new ArrayList<>();
            rankedSubcontents.add(new MovieContentRank.MovieRankedSubcontent(new MovieContentRank(MovieContentRank.Rank.TITLE), this.title));
            if (this.releaseDate != null) { // may be null
                rankedSubcontents.add(new MovieContentRank.MovieRankedSubcontent(new MovieContentRank(MovieContentRank.Rank.RELEASE_DATE), String.valueOf(this.releaseDate)));
            }
            rankedSubcontents.add(new MovieContentRank.MovieRankedSubcontent(new MovieContentRank(MovieContentRank.Rank.BOX_OFFICE_REVENUE), String.valueOf(this.boxOfficeRevenue)));
            rankedSubcontents.add(new MovieContentRank.MovieRankedSubcontent(new MovieContentRank(MovieContentRank.Rank.RUNNING_TIME), String.valueOf(this.runningTime)));
            rankedSubcontents.add(new MovieContentRank.MovieRankedSubcontent(new MovieContentRank(MovieContentRank.Rank.LANGUAGE), this.languageKeys.stream().map(languages::get).collect(Collectors.joining())));
            rankedSubcontents.add(new MovieContentRank.MovieRankedSubcontent(new MovieContentRank(MovieContentRank.Rank.COUNTRY), this.countryKeys.stream().map(countries::get).collect(Collectors.joining())));
            rankedSubcontents.add(new MovieContentRank.MovieRankedSubcontent(new MovieContentRank(MovieContentRank.Rank.GENRE), this.genreKeys.stream().map(genres::get).collect(Collectors.joining())));
            rankedSubcontents.add(new MovieContentRank.MovieRankedSubcontent(new MovieContentRank(MovieContentRank.Rank.DESCRIPTION), this.description));
            content = new Content(rankedSubcontents);
        }

        super.setContent(content);   // TODO : inefficient because a new string which is the concatenation is created
    }

    public Movie() {
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
                        .parallel()
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
                .parallel()
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(
                        Collectors.toConcurrentMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (movie1, movie2) -> {// Merging function
                                    boolean areMetadataInMovie2 = movie1.title == null;
                                    Movie movieWithMetadati = areMetadataInMovie2 ? movie2 : movie1;
                                    Movie movieWithDescription = areMetadataInMovie2 ? movie1 : movie2;
                                    return new Movie(
                                            Objects.requireNonNull(movieWithMetadati.title),
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

        // Create the corpus
        return new Corpus(movies.values());

    }

    @Override
    public void writeExternal(ObjectOutput oo) throws IOException {
        for (Field f : this.getClass().getDeclaredFields()) {
            try {
                f.setAccessible(true);
                oo.writeObject(f.get(this));
            } catch (IllegalArgumentException | IllegalAccessException e) {
                Logger.getLogger(this.getClass().getCanonicalName())
                        .log(Level.SEVERE, "Impossible to serialize due to an exception", e);
            }
        }
    }

    /**
     * All fields but final fields take part in the deserialization.
     */
    @Override
    public void readExternal(ObjectInput oi) throws IOException, ClassNotFoundException {

        // Read in the same order they were written
        try {
            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            for (Field f : this.getClass().getDeclaredFields()) {
                try {
                    f.setAccessible(true);
                    Object read = oi.readObject();

                    if (Modifier.isFinal(f.getModifiers())) {
                        //modifiers.setInt(f, f.getModifiers() & ~Modifier.FINAL);    // the field will not be final anymore, if it was
//                        Logger.getLogger(this.getClass().getCanonicalName())
//                                .log(Level.WARNING, "The field " + f + " is final and was not set");
                    } else {
                        if (Modifier.isStatic(f.getModifiers())) {
                            f.set(null, read);
                        } else {
                            f.set(this, read);
                        }
                    }
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    Logger.getLogger(this.getClass().getCanonicalName())
                            .log(Level.SEVERE, "Impossible to deserialize due to an exception", e);
                }
            }
        } catch (NoSuchFieldException | SecurityException ex) {
            Logger.getLogger(this.getClass().getCanonicalName()).log(Level.SEVERE, "Exception thrown during the deserialization.", ex);
        }
    }

    /**
     * @return The movie title followed by (one tab spaced) the JSON representation of the object.
     */
    @Override
    public String toString() {

        assert title != null;
        assert languageKeys != null;
        assert countryKeys != null;
        assert genreKeys != null;

        Utility.TriFunction<List<String>, String, Map<String, String>, String> keyListToValueListAsString = (keyList, fieldName, mapKeyToValue) -> {
            List<String> values = keyList.stream()
                    .filter(Objects::nonNull)
                    .map(mapKeyToValue::get)
                    .filter(Objects::nonNull)
                    .map(value -> "\"" + mapKeyToValue.get(value) + "\"")
                    .collect(Collectors.toList());
            return values.size() > 0 ? (", " + fieldName + ": " + values) : "";
        };

        String languages_ = keyListToValueListAsString.apply(languageKeys, "languageKeys", languages);
        String countries_ = keyListToValueListAsString.apply(countryKeys, "countryKeys", countries);
        String genres_ = keyListToValueListAsString.apply(genreKeys, "genreKeys", genres);

        return title + "\t{" +
                "title: \"" + title.replaceAll("\"", "'") + "\"" +
                (releaseDate != null ? (", releaseDate: \"" + releaseDate.atStartOfDay() + ":00Z\"") : "") +
                (boxOfficeRevenue > 0 ? (", boxOfficeRevenue: \"" + boxOfficeRevenue + " $\"") : "") +
                (runningTime > 0 ? (", runningTime: " + runningTime) : "") +
                (languages_.isEmpty() ? "" : languages_) +
                (countries_.isEmpty() ? "" : countries_) +
                (genres_.isEmpty() ? "" : genres_) +
                (description != null && !description.trim().isEmpty() ? (", description: \"" + description.replaceAll("\"", "'") + "\"") : "") +
                "}";
    }

    /**
     * Class implementing {@link components.Document.Content.RankedSubcontent.ContentRank} for {@link Movie}s.
     */
    private static class MovieContentRank implements Content.RankedSubcontent.ContentRank {
        /**
         * The rank.
         */
        private final Rank rank;

        MovieContentRank(@NotNull final Rank rank) {
            this.rank = Objects.requireNonNull(rank);
        }

        @Override
        public int compareTo(@NotNull Document.Content.RankedSubcontent.ContentRank otherContentRank) {
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
         * Concrete cass extending {@link components.Document.Content.RankedSubcontent}.
         */
        static class MovieRankedSubcontent extends Content.RankedSubcontent {

            /**
             * Constructor.
             *
             * @param rank       The rank for this subcontent.
             * @param subcontent The subcontent.
             */
            public MovieRankedSubcontent(@NotNull ContentRank rank, @NotNull String subcontent) {
                super(Objects.requireNonNull(rank), Objects.requireNonNull(subcontent));
            }

            private static int sumRanks(@NotNull Document.Content.RankedSubcontent first, @NotNull Document.Content.RankedSubcontent second) {
                return sumRanks(
                        ((MovieContentRank) first.getRank()).rank.getRankValue(),
                        ((MovieContentRank) second.getRank()).rank.getRankValue()
                );
            }

            private static int sumRanks(int rank1, int rank2) {
                return (Rank.values().length - rank1) + (Rank.values().length - rank2);
            }

            @Override
            public int sum(@NotNull Document.Content.RankedSubcontent rankedSubcontent) {
                return sumRanks(this, rankedSubcontent);
            }

            @Override
            public int sum(@NotNull Collection<Content.RankedSubcontent> rankedSubcontents) {
                return Objects.requireNonNull(rankedSubcontents)
                        .stream().unordered()
                        .map(aRank -> ((MovieContentRank) aRank.getRank()).rank.getRankValue())
                        .reduce(MovieRankedSubcontent::sumRanks)
                        .orElse(0);
            }
        }
    }
}