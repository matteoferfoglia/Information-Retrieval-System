package it.units.informationretrieval.ir_boolean_model.utils.skiplist;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class SkipListTest {

    private static final List<SkipListElement<Integer>> sampleListWithDuplicatesUnordered =
            FakeSkipListElement.fromElements(new Integer[]{1, 1, 2, 9, 8, 5, 2, 1, 6, 2, 99, -1, 0, 5});
    private static final List<SkipListElement<Integer>> correspondingOrderedListWithoutDuplicates =
            FakeSkipListElement.fromElements(new Integer[]{-1, 0, 1, 2, 5, 6, 8, 9, 99});
    private static SkipList<Integer> skipList;

    @BeforeEach
    void createEmptySkipList() {
        skipList = new SkipList<>(new ArrayList<>());
    }

    private static List<Integer> getExpectedPositionOfForwardPointers(int listSize) {
        int expectedNumberOfForwardPointers = (int) Math.ceil(Math.sqrt(listSize));
        return IntStream.range(0, listSize)
                .filter(i -> i % expectedNumberOfForwardPointers == 0)
                .filter(i -> i < listSize - 1) // last posting is never a forward pointer
                .boxed()
                .toList();
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

    private void assertThatForwardPointersAreCorrectlySet() {
        assertThatCorrectNumberOfForwardPointersIsPresent();
        assertThatForwardPointersAreSetAtCorrectPositions();
    }

    @Test
    void createSkipListAndAssertThatIsSortedAndWithoutDuplicates() {
        skipList = new SkipList<>(sampleListWithDuplicatesUnordered);
        assertEquals(correspondingOrderedListWithoutDuplicates, skipList.getList());
    }

//    @ParameterizedTest    // TODO: re-do this test
//    @ValueSource(booleans = {true, false})
//    void hasFirstElementForwardPointer(boolean hasForwardPointer) throws NoSuchFieldException, IllegalAccessException {
//        skipList.add(0);
//        assert skipList.size() > 0;   // pre-condition for this test
//        if (hasForwardPointer) {
//            Field forwardPointerField = skipList.getClass().getDeclaredField("forwardPointers");
//            forwardPointerField.setAccessible(true);
//            @SuppressWarnings("unchecked")// forwardPointers are of the same type as elements in the skipList
//            var forwardPointerList = (List<Integer>) forwardPointerField.get(skipList);
//            assert forwardPointerList.size() > 0;
//            forwardPointerList.set(0, skipList.get(0)/*any non-null value is ok*/);
//        }
//        assertEquals(hasForwardPointer, skipList.get(0).hasForwardPointer());
//    }

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