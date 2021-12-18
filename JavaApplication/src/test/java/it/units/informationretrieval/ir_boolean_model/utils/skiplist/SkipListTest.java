package it.units.informationretrieval.ir_boolean_model.utils.skiplist;

import benchmark.Benchmark;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class SkipListTest {

    private static final List<SkipListElement<Integer>> sampleListWithDuplicatesUnordered =
            FakeSkipListElement.fromElements(new Integer[]{1, 1, 2, 9, 8, 5, 2, 1, 6, 2, 99, -1, 0, 5});
    private static final List<SkipListElement<Integer>> correspondingOrderedListWithoutDuplicates =
            FakeSkipListElement.fromElements(new Integer[]{-1, 0, 1, 2, 5, 6, 8, 9, 99});
    private static SkipList<Integer> skipList;

    private static final int LIST_SIZE_FOR_BENCHMARK = 10000;
    private static final List<SkipListElement<Integer>> LIST_FOR_BENCHMARK =
            IntStream.range(0, LIST_SIZE_FOR_BENCHMARK)
                    .mapToObj(FakeSkipListElement::new)
                    .map(el -> (SkipListElement<Integer>) el)
                    .toList();
    private static final Supplier<Integer> randomIndexFromLargeSkipListSupplier = new Supplier<>() {
        private static final List<Integer> permutationOfIndexes =
                IntStream.range(0, LIST_SIZE_FOR_BENCHMARK).boxed().collect(Collectors.toList());
        private static int counter = 0;

        static {
            Collections.shuffle(permutationOfIndexes);
        }

        @Override
        public Integer get() {
            return permutationOfIndexes.get(counter++ % LIST_SIZE_FOR_BENCHMARK);
        }
    };
    private static final Supplier<SkipListElement<Integer>> randomSkipListElementSupplier = new Supplier<>() {
        private static final List<SkipListElement<Integer>> permutationOfElements =
                IntStream.range(0, LIST_SIZE_FOR_BENCHMARK)
                        .mapToObj(FakeSkipListElement::new)
                        .collect(Collectors.toList());
        private static int counter = 0;

        static {
            Collections.shuffle(permutationOfElements);
        }

        @Override
        public SkipListElement<Integer> get() {
            return permutationOfElements.get(counter++ % LIST_SIZE_FOR_BENCHMARK);
        }
    };
    private static final String CANONICAL_NAME_OF_SKIP_LIST_INITIALIZER_FOR_BENCHMARK = // pay attention if the path change!
            "it.units.informationretrieval.ir_boolean_model.utils.skiplist.SkipListTest.initializeLargeSkipListForBenchmark";
    private static SkipList<Integer> largeSkipListForBenchmark;

    static {
        initializeLargeSkipListForBenchmark();
    }

    private static void initializeLargeSkipListForBenchmark() {
        largeSkipListForBenchmark = new SkipList<>(LIST_FOR_BENCHMARK);
    }

    private static List<Integer> getExpectedPositionOfForwardPointers(int listSize) {
        int expectedNumberOfForwardPointers = (int) Math.ceil(Math.sqrt(listSize));
        return IntStream.range(0, listSize)
                .filter(i -> i % expectedNumberOfForwardPointers == 0)
                .filter(i -> i < listSize - 1) // last posting is never a forward pointer
                .boxed()
                .toList();
    }

    @Benchmark(afterEach = CANONICAL_NAME_OF_SKIP_LIST_INITIALIZER_FOR_BENCHMARK/* re-set setup conditions */)
    static void addListOfSkipListElements() {
        largeSkipListForBenchmark.addAll(LIST_FOR_BENCHMARK);
    }

    @Benchmark
    static void testSize() {
        largeSkipListForBenchmark.size();
    }

    @Benchmark(commentToReport = "Take one random element from the list")
    static void get() {
        largeSkipListForBenchmark.get(randomIndexFromLargeSkipListSupplier.get());
    }

    private void assertThatForwardPointersAreSetAtCorrectPositions() {
        int P = skipList.size();
        List<Integer> expectedPositionOfForwardPointers = getExpectedPositionOfForwardPointers(P);
        List<Integer> actualPositionOfForwardPointers = IntStream.range(0, P)
                .filter(i -> skipList.get(i).hasForwardPointer())
                .boxed()
                .toList();
        assertEquals(expectedPositionOfForwardPointers, actualPositionOfForwardPointers);
    }

    private void assertThatCorrectNumberOfForwardPointersIsPresent() {
        int expectedNumberOfForwardPointers = getExpectedPositionOfForwardPointers(skipList.size()).size();
        int actualNumberOfForwardPointers =
                (int) IntStream.range(0, skipList.size())
                        .filter(i -> skipList.get(i).hasForwardPointer())
                        .count();
        assertEquals(expectedNumberOfForwardPointers, actualNumberOfForwardPointers);
    }

    @Test
    void createSkipListAndAssertThatCorrectNumberOfForwardPointersAreSet() {
        skipList = new SkipList<>(sampleListWithDuplicatesUnordered);
        assertThatCorrectNumberOfForwardPointersIsPresent();
    }

    @Test
    void createSkipListAndAssertThatForwardPointersAreSetAtCorrectPosition() {
        skipList = new SkipList<>(sampleListWithDuplicatesUnordered);
        assertThatForwardPointersAreSetAtCorrectPositions();
    }

    @Test
    void createSkipListAndAssertThatIsSortedAndWithoutDuplicates() {
        skipList = new SkipList<>(sampleListWithDuplicatesUnordered);
        assertEquals(correspondingOrderedListWithoutDuplicates, skipList.getList());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 10, 1000})
    void size(int numberOfElementsToAdd) {
        IntStream.range(0, numberOfElementsToAdd)
                .forEach(i -> skipList.add(new FakeSkipListElement<>(i)));
        assertEquals(numberOfElementsToAdd, skipList.size());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 10, 1000})
    void addSkipList(int numberOfElementsToAdd) {
        skipList.addAll(IntStream.range(0, numberOfElementsToAdd).mapToObj(FakeSkipListElement::new).map(el -> (SkipListElement<Integer>) el).toList());
        assertEquals(numberOfElementsToAdd, skipList.size());
        assertThatForwardPointersAreCorrectlySet();
    }

    @Test
    void toUnmodifiableList() {
        var unmodifiableList = new SkipList<>(sampleListWithDuplicatesUnordered).toUnmodifiableList();
        assertEquals(correspondingOrderedListWithoutDuplicates.stream().map(SkipListElement::getElement).toList(), unmodifiableList);
        try {
            //noinspection ConstantConditions   // the test asserts that the list is unmodifiable
            unmodifiableList.add(1);
            fail("Unmodifiable list modified but should not happen");
        } catch (Exception ignored) {
            // correct that an exception is thrown if trying to modify an unmodifiable list
        }
    }

    @Test
    void testToString() {
        assertEquals(
                correspondingOrderedListWithoutDuplicates.toString(),
                new SkipList<>(sampleListWithDuplicatesUnordered).getList().toString());
    }

    @Benchmark(afterEach = CANONICAL_NAME_OF_SKIP_LIST_INITIALIZER_FOR_BENCHMARK/* re-set setup conditions */)
    static void addSkipList() {
        largeSkipListForBenchmark.addAll(largeSkipListForBenchmark);
    }

    private void assertThatForwardPointersAreCorrectlySet() {
        assertThatCorrectNumberOfForwardPointersIsPresent();
        assertThatForwardPointersAreSetAtCorrectPositions();
    }

    @Benchmark(afterEach = CANONICAL_NAME_OF_SKIP_LIST_INITIALIZER_FOR_BENCHMARK/* re-set setup conditions */)
    static void add() {
        largeSkipListForBenchmark.add(randomSkipListElementSupplier.get());
    }

    @Benchmark
    static void toUnmodifiableListBenchmark() {
        largeSkipListForBenchmark.toUnmodifiableList();
    }

    @BeforeEach
    void createEmptySkipList() {
        skipList = new SkipList<>();
    }

    @Test
    void createEmptySkipListAndAssertItsSizeIsZero() {
        assertEquals(0, new SkipList<>().size());
    }
}

class FakeSkipListElement<T extends Comparable<T>> implements SkipListElement<T> {

    T element;
    SkipListElement<T> forwardPointer;
    int forwardedElementIndex;

    FakeSkipListElement(T element, int forwardedElementIndex, SkipListElement<T> forwardPointer) {
        this.element = element;
        this.forwardedElementIndex = forwardedElementIndex;
        this.forwardPointer = forwardPointer;
    }

    FakeSkipListElement(T element) {
        this(element, -1, null);
    }

    static <T extends Comparable<T>> List<SkipListElement<T>> fromElements(T[] elements) {
        return Arrays.stream(elements)
                .map(FakeSkipListElement::new)
                .map(el -> (SkipListElement<T>) el)
                .toList();
    }

    @Override
    public @NotNull SkipListElement<T> setForwardPointer(int i, @Nullable SkipListElement<T> e) {
        this.forwardPointer = e;
        this.forwardedElementIndex = i;
        return this;
    }

    @Override
    public @Nullable SkipListElement<T> getForwardedElement() {
        return forwardPointer;
    }

    @Override
    public boolean hasForwardPointer() {
        return forwardPointer != null;
    }

    @Override
    public T getElement() {
        return element;
    }

    @Override
    public int getForwardedIndex() {
        return forwardedElementIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FakeSkipListElement<?> that = (FakeSkipListElement<?>) o;

        return Objects.equals(element, that.element);
    }

    @Override
    public int hashCode() {
        return element != null ? element.hashCode() : 0;
    }

    @Override
    public int compareTo(@NotNull T o) {
        return element.compareTo(((FakeSkipListElement<T>) o).getElement());
    }

    @Override
    public String toString() {
        return "FakeSkipListElement{" + "element=" + element + '}';
    }
}