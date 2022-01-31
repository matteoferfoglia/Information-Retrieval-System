package it.units.informationretrieval.ir_boolean_model.utils.wildcards;

import it.units.informationretrieval.ir_boolean_model.entities.InvertedIndex;
import it.units.informationretrieval.ir_boolean_model.entities.Language;
import it.units.informationretrieval.ir_boolean_model.utils.Utility;
import it.units.informationretrieval.ir_boolean_model.utils.stemmers.Stemmer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Map;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import static it.units.informationretrieval.ir_boolean_model.utils.wildcards.MatcherForPermutermIndex.States.*;
import static it.units.informationretrieval.ir_boolean_model.utils.wildcards.MatcherForPermutermIndex.Symbols.*;

/**
 * The aim of this class is, given an un-stemmed wildcard query
 * and a stemmed token, to return true if the stemmed version of
 * the wildcard query is compatible with the given stemmed token,
 * false otherwise.
 * This feature can be used to answer wildcard queries, when the
 * permuterm index is used to answer to query containing more than
 * one wildcard, too (during the procedure, after the permuterm
 * index returns a number of possible tokens matching the input
 * wildcard query string -but only the first wildcard-, a way to
 * find which of that tokens match the entire query string
 * -considering all its wildcards- was needed, and this class
 * solve the job).
 * This problem is solved using an implementation of a modified
 * finite-state machine, which makes use of a memory too.
 *
 * @author Matteo Ferfoglia
 */
public class MatcherForPermutermIndex {

    /**
     * Transition matrix for the finite-state machine.
     */
    @NotNull
    private static final States[][] transitionMtx = {
            /*            START, NORMAL, WILDCARD, VALID, INVALID, INVALID_TMP, TMP, RECOVERY, SAVE */
            /*unstable*/ {INVALID, INVALID, INVALID, null, null, INVALID, INVALID, INVALID, INVALID},
            /* 1      */ {NORMAL, NORMAL, INVALID, INVALID, INVALID, INVALID, INVALID, NORMAL, NORMAL},
            /* 2      */ {WILDCARD, WILDCARD, INVALID, INVALID, INVALID, INVALID, INVALID, WILDCARD, WILDCARD},
            /* 3      */ {INVALID, INVALID, WILDCARD, INVALID, INVALID, INVALID, INVALID, INVALID, INVALID},
            /* 4      */ {INVALID, SAVE, SAVE, INVALID, INVALID, INVALID, INVALID, SAVE, SAVE},
            /* 5      */ {VALID, VALID, INVALID, INVALID, INVALID, INVALID, INVALID, VALID, VALID},
            /* 6      */ {INVALID, VALID, INVALID, INVALID, INVALID, INVALID, INVALID, VALID, VALID},
            /* 7      */ {INVALID, INVALID_TMP, INVALID, INVALID, INVALID, INVALID, INVALID, INVALID_TMP, INVALID_TMP},
            /* 8      */ {INVALID, INVALID_TMP, INVALID, INVALID, INVALID, INVALID, INVALID, INVALID_TMP, INVALID_TMP},
            /* 9      */ {INVALID, INVALID, INVALID, INVALID, INVALID, INVALID, INVALID, INVALID, INVALID},
            /*10      */ {INVALID, INVALID, INVALID, INVALID, INVALID, RECOVERY, INVALID, INVALID, INVALID},
            /*11      */ {INVALID, TMP, INVALID_TMP, INVALID, INVALID, INVALID, INVALID, TMP, TMP},
            /*12      */ {INVALID, INVALID_TMP, INVALID, INVALID, INVALID, INVALID, INVALID, INVALID_TMP, INVALID_TMP},
            /*13      */ {INVALID, INVALID, INVALID, INVALID, INVALID, INVALID, VALID, INVALID, INVALID},
            /*14      */ {INVALID, INVALID, INVALID, INVALID, INVALID, INVALID, VALID, INVALID, INVALID},
            /*15      */ {INVALID, INVALID, INVALID, INVALID, INVALID, INVALID, INVALID_TMP, INVALID, INVALID}
    };
    /**
     * The wildcard un-stemmed query string.
     */
    @NotNull
    private final String q;
    /**
     * The eventually (according to system settings) stemmed token from the {@link InvertedIndex}.
     */
    private final String t;
    /**
     * The {@link Stack} used by the finite-state machine.
     */
    private final Stack<Map.Entry<Integer, Integer>> S = new Stack<>();
    /**
     * The {@link Language} for {@link #q} and {@link #t}.
     */
    private final Language language;
    /**
     * The index to scan {@link #q}.
     */
    private int i = 0;
    /**
     * The index to scan {@link #t}.
     */
    private int j = 0;
    /**
     * The result of the evaluation.
     */
    @Nullable // becomes not null when the result is ready.
    private Results result;
    /**
     * The inner current state of the finite-state machine.
     */
    private States currentState;

    /**
     * Constructor.
     *
     * @param wildcardQuerystring   The wildcard querystring.
     * @param stemmedTokenFromIndex The token obtained from the {@link InvertedIndex}
     *                              for which you want to see if it is compatible with
     *                              the input wildcard query string.
     * @param language              The {@link Language} for the wildcard querystring and the token.
     */
    public MatcherForPermutermIndex(
            @NotNull String wildcardQuerystring, @NotNull String stemmedTokenFromIndex, @NotNull Language language) {
        assert transitionMtx.length == Symbols.values().length;
        assert Arrays.stream(transitionMtx).allMatch(aRow -> aRow.length == States.values().length);

        this.q = wildcardQuerystring;
        this.t = stemmedTokenFromIndex;
        this.language = language;

        this.currentState = START;
    }

    /**
     * @param unstemmedInputWildcardQuery The input wildcard query string.
     * @param stemmedTokenFromIndex       An eventually stemmed token obtained from the {@link InvertedIndex}.
     * @param language                    The {@link Language} for the wildcard querystring and the token.
     * @return true if the stemmed token is compatible with the input wildcard query.
     */
    public static boolean isWildcardQueryCompatibleWithStemmedTokenFromIndex(
            @NotNull String unstemmedInputWildcardQuery, @NotNull String stemmedTokenFromIndex, @NotNull Language language) {
        return new MatcherForPermutermIndex(unstemmedInputWildcardQuery, stemmedTokenFromIndex, language)
                .getResult().equals(Results.VALID);
    }

    /**
     * Starts the finite-state machine and makes it to evolve
     * to converge on the result.
     * When this method ends, the result of the computation
     * will be available in {@link #result}.
     */
    private void accept() {
        long TIMEOUT_MILLIS = 20;
        long START_MILLIS = System.currentTimeMillis();
        try {
            while (result == null) {
                if (System.currentTimeMillis() - START_MILLIS > TIMEOUT_MILLIS) {
                    throw new RuntimeException(
                            "The finite-state machine did not converge within " + TIMEOUT_MILLIS + " ms."
                                    + System.lineSeparator() + "\tCurrent instance: " + this);
                }
                switch (currentState) {
                    case NORMAL:
                        i++;
                        j++;
                        break;
                    case WILDCARD:
                        j++;
                        break;
                    case SAVE:
                        i++;
                        S.push(new AbstractMap.SimpleEntry<>(i, j));
                        break;
                    case RECOVERY:
                        AbstractMap.SimpleEntry<Integer, Integer> ij =
                                (AbstractMap.SimpleEntry<Integer, Integer>) S.pop();
                        i = ij.getKey();
                        j = ij.getValue() + 1;
                    case VALID:
                    case INVALID:
                        result = currentState == VALID ? Results.VALID : Results.INVALID;
                        break;
                    default:
                        break;
                }
                currentState = transitionMtx[States.getNextSymbol(this).ordinal()][currentState.ordinal()];
            }
        } catch (RuntimeException e) {
            Logger.getLogger(getClass().getCanonicalName()).log(Level.WARNING, e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        return "MatcherForPermutermIndex {q=" + q + ", t=" + t + ", i=" + i + ", j=" + j
                + ", currentState=" + currentState + ", S=" + S + "}";
    }

    /**
     * @return the result of the computation provided by the finite-state machine.
     */
    @NotNull
    private Results getResult() {
        if (result == null) {
            // if not already evaluated, compute the result
            accept();
        }
        return result;
    }

    /**
     * Enumeration of states for the finite-state machine
     */
    enum States {
        /**
         * Starting state.
         */
        START,
        /**
         * Normal linear scansion of both the input strings, while characters are the same in both.
         */
        NORMAL,
        /**
         * When a wildcard is found.
         */
        WILDCARD,
        /**
         * If {@link #t} is compatible with the input wildcard query string {@link #q}.
         */
        VALID,
        /**
         * If {@link #t} is <strong>not</strong> compatible with the input wildcard query string {@link #q}.
         */
        INVALID,
        /**
         * If {@link #t} might be <strong>not</strong> compatible with the input wildcard query string {@link #q},
         * but further evolutions of the finite-state machine are needed (e.g., there might be another
         * scan of {@link #t} that starting from another character (after a wildcard was encountered
         * in {@link #q}) might lead to a compatibility condition).
         */
        INVALID_TMP,
        /**
         * Similar to {@link #INVALID_TMP}: according to further step of the finite-state machine
         * we will get the result.
         */
        TMP,
        /**
         * Recover a previous step (if a wildcard was encountered).
         */
        RECOVERY,
        /**
         * Save the position at which a wildcard was encountered.
         */
        SAVE;

        /**
         * @param m the {@link MatcherForPermutermIndex finite-state machine}.
         * @return the next {@link Symbols input} for the finite-state machine,
         * according to the current state and the possible evolutions.
         */
        static Symbols getNextSymbol(MatcherForPermutermIndex m) {
            switch (m.currentState) {
                case START:
                    if (m.i < m.q.length() && m.j < m.t.length() && m.q.charAt(m.i) == m.t.charAt(m.j)) {
                        return _01;
                    } else if (m.i < m.q.length() - 1 && m.j < m.t.length() && m.q.charAt(m.i) != m.t.charAt(m.j) && String.valueOf(m.q.charAt(m.i)).equals(InvertedIndex.WILDCARD)) {
                        return _02;
                    } else if (m.i == m.q.length() && m.j == m.t.length()) {
                        return _05;
                    } else {
                        throw new IllegalStateException("Unexpected configuration " + m);
                    }
                case NORMAL:
                case RECOVERY:
                case SAVE:
                    if (m.i < m.q.length() && m.j < m.t.length() && m.q.charAt(m.i) == m.t.charAt(m.j)) {
                        return _01;
                    } else if (m.i < m.q.length() - 1 && m.j < m.t.length() && m.q.charAt(m.i + 1) == m.t.charAt(m.j)) {
                        return _04;
                    } else if (m.i < m.q.length() - 1 && m.j < m.t.length() && m.q.charAt(m.i) != m.t.charAt(m.j) && String.valueOf(m.q.charAt(m.i)).equals(InvertedIndex.WILDCARD)) {
                        return _02;
                    } else if (m.i == m.q.length() && m.j == m.t.length()) {
                        return _05;
                    } else if (m.i == m.q.length() - 1 && m.j < m.t.length() && m.q.charAt(m.i) != m.t.charAt(m.j) && String.valueOf(m.q.charAt(m.i)).equals(InvertedIndex.WILDCARD)) {
                        return _06;
                    } else if (m.i == m.q.length() - 1 && m.j < m.t.length() && m.q.charAt(m.i) != m.t.charAt(m.j) && !String.valueOf(m.q.charAt(m.i)).equals(InvertedIndex.WILDCARD)) {
                        return _07;
                    } else if (m.i < m.q.length() - 1 && m.j < m.t.length() && m.q.charAt(m.i) != m.t.charAt(m.j) && !String.valueOf(m.q.charAt(m.i)).equals(InvertedIndex.WILDCARD)) {
                        return _08;
                    } else if (m.i < m.q.length() && m.j == m.t.length()) {
                        return _11;
                    } else if (m.i == m.q.length() && m.j < m.t.length()) {
                        return _12;
                    } else {
                        throw new IllegalStateException("Unexpected configuration " + m);
                    }
                case WILDCARD:
                    if (m.i < m.q.length() - 1 && m.j < m.t.length() && m.q.charAt(m.i + 1) != m.t.charAt(m.j)) {
                        return _03;
                    } else if (m.i < m.q.length() - 1 && m.j < m.t.length() && m.q.charAt(m.i + 1) == m.t.charAt(m.j)) {
                        return _04;
                    } else if (m.i < m.q.length() && m.j == m.t.length()) {
                        return _11;
                    } else {
                        throw new IllegalStateException("Unexpected configuration " + m);
                    }
                case VALID:
                case INVALID:
                    return __0;
                case INVALID_TMP:
                    if (m.S.isEmpty()) {
                        return _09;
                    } else //noinspection ConstantConditions // explicit condition in if
                        if (!m.S.isEmpty()) {
                            return _10;
                        } else {
                            throw new IllegalStateException("Unexpected configuration " + m);
                        }
                case TMP:
                    if (String.valueOf(m.q.charAt(m.i)).equals(InvertedIndex.WILDCARD) && m.i == m.q.length() - 1) {
                        return _13;
                    } else if ((Utility.getStemmer() == null
                            ? Stemmer.getStemmer(Stemmer.AvailableStemmer.NO_STEMMING)
                            : Utility.getStemmer())
                            .stem(m.t + m.q.substring(m.i).replaceAll("\\*", ""), m.language)
                            .equals(m.t)) {
                        // stemming of the residual of the wildcard query (assuming that the token from the dictionary
                        //  was the correct substitution for the wildcard) leads to the token itself
                        return _14;
                    } else if (!String.valueOf(m.q.charAt(m.i)).equals(InvertedIndex.WILDCARD) || m.i < m.q.length() - 1) {
                        return _15;
                    } else {
                        throw new IllegalStateException("Unexpected configuration " + m);
                    }
                default:
                    throw new IllegalStateException("Unexpected configuration " + m);
            }
        }

    }

    /**
     * Enumeration of symbols for the finite-state machine.
     */
    enum Symbols {__0/*for unstable states*/, _01, _02, _03, _04, _05, _06, _07, _08, _09, _10, _11, _12, _13, _14, _15}

    /**
     * Enumeration for the possible results.
     */
    private enum Results {VALID, INVALID}
}