package com.mewebstudio.springboot.jpa.nestedset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Test for the Pair class.")
public class PairTest {
    @Test
    public void testPairCreation() {
        // Arrange
        Integer firstValue = 1;
        String secondValue = "Hello";

        // Act
        Pair<Integer, String> pair = new Pair<>(firstValue, secondValue);

        // Assert
        assertEquals(firstValue, pair.first(), "First value should be 1");
        assertEquals(secondValue, pair.second(), "Second value should be 'Hello'");
    }

    @Test
    public void testPairEquality() {
        // Arrange
        Integer firstValue = 10;
        String secondValue = "World";
        Pair<Integer, String> pair1 = new Pair<>(firstValue, secondValue);
        Pair<Integer, String> pair2 = new Pair<>(firstValue, secondValue);

        // Assert
        assertEquals(pair1, pair2, "Pairs should be equal because they have the same values.");
    }

    @Test
    public void testPairHashCode() {
        // Arrange
        Integer firstValue = 100;
        String secondValue = "Test";
        Pair<Integer, String> pair = new Pair<>(firstValue, secondValue);

        // Assert
        assertEquals(pair.hashCode(), new Pair<>(firstValue, secondValue).hashCode(), "Hash codes should match.");
    }
}
