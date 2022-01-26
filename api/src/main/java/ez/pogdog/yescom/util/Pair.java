package ez.pogdog.yescom.util;

import java.util.Objects;

/**
 * A pair of objects.
 * @param <T> The type of the first object.
 * @param <R> The type of the second object.
 */
public class Pair<T, R> {

    private final T first;
    private final R second;

    public Pair(T first, R second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || getClass() != other.getClass()) return false;
        Pair<?, ?> pair = (Pair<?, ?>)other;
        return Objects.equals(first, pair.first) && Objects.equals(second, pair.second);
    }

    @Override
    public int hashCode() {
        return Objects.hash(first, second);
    }

    @Override
    public String toString() {
        return String.format("Pair(first=%s, second=%s)", first, second);
    }

    public T getFirst() {
        return first;
    }

    public R getSecond() {
        return second;
    }
}
