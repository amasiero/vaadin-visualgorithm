package me.amasiero.visualgorithm.sorting;

public record SortingStep(Type type, int i, int j) {
    public enum Type { COMPARE, SWAP, MARK_FINAL}
    public static SortingStep compare(int i, int j) { return new SortingStep(Type.COMPARE, i, j); }
    public static SortingStep swap(int i, int j) { return new SortingStep(Type.SWAP, i, j); }
    public static SortingStep markFinal(int i) { return new SortingStep(Type.MARK_FINAL, i, -1); }
}
