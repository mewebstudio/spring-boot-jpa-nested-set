package com.mewebstudio.springboot.jpa.nestedset;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility class for working with nested set trees.
 * This class provides methods to build a tree structure from a list of nested set nodes.
 */
public class NestedSetUtil {
    /**
     * Private constructor to prevent instantiation.
     */
    private NestedSetUtil() {
        // Prevent instantiation
    }

    /**
     * Build a tree structure from a list of nested set nodes.
     *
     * @param nodes   List of nested set nodes.
     * @param convert Function to convert a node to its response type.
     * @param <ID>    Type of the node identifier.
     * @param <E>     Type of the nested set node.
     * @param <T>     Type of the response node.
     * @return List of top-level nodes with their children.
     */
    public static <ID, E extends INestedSetNode<ID>, T extends INestedSetNodeResponse<ID>> List<T> tree(
        List<E> nodes,
        Function<E, T> convert
    ) {
        if (nodes.isEmpty()) {
            return Collections.emptyList();
        }

        Map<ID, T> responseMap = new HashMap<>();
        Map<ID, E> nodeMap = nodes.stream().collect(Collectors.toMap(INestedSetNode::getId, node -> node));

        // Map nodes to their response objects
        for (E node : nodes) {
            responseMap.put(node.getId(), convert.apply(node));
        }

        // Group children by parent ID
        Map<ID, List<ID>> childrenByParentId = new HashMap<>();
        for (E node : nodes) {
            INestedSetNode<ID> parent = node.getParent();
            ID parentId = parent != null ? parent.getId() : null;
            if (parentId != null && nodeMap.containsKey(parentId)) {
                childrenByParentId.computeIfAbsent(parentId, k -> new ArrayList<>()).add(node.getId());
            }
        }

        // Build tree by assigning children to parents
        List<E> sortedNodes = nodes.stream()
            .sorted((a, b) -> Integer.compare(b.getLeft(), a.getLeft()))
            .toList();

        for (E node : sortedNodes) {
            T parent = responseMap.get(node.getId());
            List<T> children = Optional.ofNullable(childrenByParentId.get(node.getId()))
                .orElse(Collections.emptyList())
                .stream()
                .map(responseMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            @SuppressWarnings("unchecked")
            List<INestedSetNodeResponse<ID>> childrenAsResponse = (List<INestedSetNodeResponse<ID>>) children;

            @SuppressWarnings("unchecked")
            T withChildren = (T) parent.withChildren(childrenAsResponse);

            responseMap.put(node.getId(), withChildren);
        }

        // Find top-level nodes among the provided list
        return nodes.stream()
            .filter(node -> {
                INestedSetNode<ID> parent = node.getParent();
                ID parentId = parent != null ? parent.getId() : null;
                return parentId == null || !nodeMap.containsKey(parentId);
            })
            .map(node -> responseMap.get(node.getId()))
            .filter(Objects::nonNull)
            .toList();
    }
}
