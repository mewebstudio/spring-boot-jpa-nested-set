package com.mewebstudio.springboot.jpa.nestedset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Test for the MoveNodeDirection enum.")
class MoveNodeDirectionTest {
    @Test
    void testEnumValues() {
        assertEquals(2, MoveNodeDirection.values().length);
        assertEquals(MoveNodeDirection.UP, MoveNodeDirection.valueOf("UP"));
        assertEquals(MoveNodeDirection.DOWN, MoveNodeDirection.valueOf("DOWN"));
    }

    @Test
    void testEnumToString() {
        assertEquals("UP", MoveNodeDirection.UP.toString());
        assertEquals("DOWN", MoveNodeDirection.DOWN.toString());
    }

    @Test
    void testEnumEquality() {
        assertSame(MoveNodeDirection.UP, MoveNodeDirection.valueOf("UP"));
        assertSame(MoveNodeDirection.DOWN, MoveNodeDirection.valueOf("DOWN"));
    }
}
