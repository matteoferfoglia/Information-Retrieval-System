package it.units.informationretrieval.ir_boolean_model.utils.skiplist;

import it.units.informationretrieval.ir_boolean_model.utils.SkipList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class SkipListTest {

    private static final Integer[] sampleArrayWithDuplicatesUnordered = {1, 1, 2, 9, 8, 5, 2, 1, 6, 2, 99, -1, 0, 5};
    private static final Integer[] correspondingOrderedArrayWithoutDuplicates = {-1, 0, 1, 2, 5, 6, 8, 9, 99};
    private static SkipList<Integer> skipList;

    @BeforeEach
    void createEmptySkipList() {
        skipList = new SkipList<>();
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
        skipList = new SkipList<>(sampleArrayWithDuplicatesUnordered);
        assertThatCorrectNumberOfForwardPointersIsPresent();
    }

    @Test
    void createSkipListAndAssertThatForwardPointersAreSetAtCorrectPosition() {
        skipList = new SkipList<>(sampleArrayWithDuplicatesUnordered);
        assertThatForwardPointersAreSetAtCorrectPositions();
    }

    private void assertThatForwardPointersAreSetAtCorrectPositions() {
        int P = skipList.size();
        List<Integer> expectedPositionOfForwardPointers = getExpectedPositionOfForwardPointers(P);
        List<Integer> actualPositionOfForwardPointers = IntStream.range(0, P)
                .filter(skipList::hasForwardPointer)
                .boxed()
                .toList();
        assertEquals(expectedPositionOfForwardPointers, actualPositionOfForwardPointers);
    }

    private void assertThatCorrectNumberOfForwardPointersIsPresent() {
        int expectedNumberOfForwardPointers = getExpectedPositionOfForwardPointers(skipList.size()).size();
        int actualNumberOfForwardPointers =
                (int) IntStream.range(0, skipList.size())
                        .filter(skipList::hasForwardPointer)
                        .count();
        assertEquals(expectedNumberOfForwardPointers, actualNumberOfForwardPointers);
    }

    private void assertThatForwardPointersAreCorrectlySet() {
        assertThatCorrectNumberOfForwardPointersIsPresent();
        assertThatForwardPointersAreSetAtCorrectPositions();
    }

    @Test
    void createSkipListAndAssertThatIsSortedAndWithoutDuplicates() {
        skipList = new SkipList<>(sampleArrayWithDuplicatesUnordered);
        assertEquals(Arrays.asList(correspondingOrderedArrayWithoutDuplicates), skipList.toUnmodifiableList());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void hasFirstElementForwardPointer(boolean hasForwardPointer) throws NoSuchFieldException, IllegalAccessException {
        skipList.add(0);
        assert skipList.size() > 0;   // pre-condition for this test
        if (hasForwardPointer) {
            Field forwardPointerField = skipList.getClass().getDeclaredField("forwardPointers");
            forwardPointerField.setAccessible(true);
            @SuppressWarnings("unchecked")// forwardPointers are of the same type as elements in the skipList
            var forwardPointerList = (List<Integer>) forwardPointerField.get(skipList);
            assert forwardPointerList.size() > 0;
            forwardPointerList.set(0, skipList.get(0)/*any non-null value is ok*/);
        }
        assertEquals(hasForwardPointer, skipList.hasForwardPointer(0));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 10, 1000})
    void size(int numberOfElementsToAdd) {
        IntStream.range(0, numberOfElementsToAdd)
                .forEach(skipList::add);
        assertEquals(numberOfElementsToAdd, skipList.size());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 10, 1000})
    void addList(int numberOfElementsToAdd) {
        skipList.addAll(IntStream.range(0, numberOfElementsToAdd).boxed().toList());
        assertEquals(numberOfElementsToAdd, skipList.size());
        assertThatForwardPointersAreCorrectlySet();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3, 10, 1000})
    void addSkipList(int numberOfElementsToAdd) {
        skipList.addAll(new SkipList<>(IntStream.range(0, numberOfElementsToAdd).boxed().toArray(Integer[]::new)));
        assertEquals(numberOfElementsToAdd, skipList.size());
        assertThatForwardPointersAreCorrectlySet();
    }

    @Test
    void toUnmodifiableList() {
        var unmodifiableList = new SkipList<>(sampleArrayWithDuplicatesUnordered).toUnmodifiableList();
        assertEquals(Arrays.asList(correspondingOrderedArrayWithoutDuplicates), unmodifiableList);
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
                Arrays.asList(correspondingOrderedArrayWithoutDuplicates).toString(),
                new SkipList<>(sampleArrayWithDuplicatesUnordered).toUnmodifiableList().toString());
    }
}