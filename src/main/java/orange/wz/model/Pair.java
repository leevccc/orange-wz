package orange.wz.model;

import lombok.Getter;

@Getter
public class Pair<E, F> {
    public E left;
    public F right;

    /**
     * Class constructor - pairs two objects together.
     *
     * @param left  The left object.
     * @param right The right object.
     */
    public Pair(E left, F right) {
        this.left = left;
        this.right = right;
    }

    /**
     * Turns the pair into a string.
     *
     * @return Each value of the pair as a string joined with a colon.
     */
    @Override
    public String toString() {
        return left.toString() + ":" + right.toString();
    }

    /**
     * Gets the hash code of this pair.
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((left == null) ? 0 : left.hashCode());
        result = prime * result + ((right == null) ? 0 : right.hashCode());
        return result;
    }

    /**
     * Checks to see if two pairs are equal.
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Pair other = (Pair) obj;
        if (left == null) {
            if (other.left != null) {
                return false;
            }
        } else if (!left.equals(other.left)) {
            return false;
        }
        if (right == null) {
            return other.right == null;
        } else return right.equals(other.right);
    }
}