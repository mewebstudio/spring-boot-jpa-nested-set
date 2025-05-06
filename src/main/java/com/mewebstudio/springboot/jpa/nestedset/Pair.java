package com.mewebstudio.springboot.jpa.nestedset;

/**
 * A simple record class to hold a pair of values.
 *
 * @param <A> Type of the first value.
 * @param <B> Type of the second value.
 */
public record Pair<A, B>(A first, B second) {
}
