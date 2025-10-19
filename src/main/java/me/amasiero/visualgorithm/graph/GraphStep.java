package me.amasiero.visualgorithm.graph;

import java.util.List;

public record GraphStep(Type type, int node, List<Integer> frontier) {
    public enum Type { VISIT, FRONTIER }
    public static GraphStep visit(int node) { return new GraphStep(Type.VISIT, node, List.of()); }
    public static GraphStep frontier(List<Integer> nodes) { return new GraphStep(Type.FRONTIER, -1, List.copyOf(nodes)); }
}
