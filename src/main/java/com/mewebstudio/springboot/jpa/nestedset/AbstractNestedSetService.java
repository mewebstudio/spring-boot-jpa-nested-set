package com.mewebstudio.springboot.jpa.nestedset;

import jakarta.transaction.Transactional;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Abstract service class for managing nested set trees.
 *
 * @param <T>  The type of the nested set node.
 * @param <ID> The type of the identifier for the nested set node.
 */
public abstract class AbstractNestedSetService<T extends INestedSetNode<ID>, ID> {
    private static final int TEMP_OFFSET = Integer.MIN_VALUE;

    /**
     * The repository to be used for database operations.
     */
    protected final JpaNestedSetRepository<T, ID> repository;

    /**
     * Constructor for AbstractNestedSetService.
     *
     * @param repository The repository to be used for database operations.
     */
    protected AbstractNestedSetService(JpaNestedSetRepository<T, ID> repository) {
        this.repository = repository;
    }

    /**
     * Get ancestors of a node.
     *
     * @param entity The node whose ancestors are to be found.
     * @return A list of ancestor nodes.
     */
    public List<T> getAncestors(T entity) {
        return repository.findAncestors(entity.getLeft(), entity.getRight());
    }

    /**
     * Get descendants of a node.
     *
     * @param entity The node whose descendants are to be found.
     * @return A list of descendant nodes.
     */
    public List<T> getDescendants(T entity) {
        return repository.findDescendants(entity.getLeft(), entity.getRight());
    }

    /**
     * Move a node up in the tree.
     *
     * @param node The node to be moved up.
     * @return The updated node.
     */
    @Transactional
    public T moveUp(T node) {
        return moveNode(node, MoveNodeDirection.UP);
    }

    /**
     * Move a node down in the tree.
     *
     * @param node The node to be moved down.
     * @return The updated node.
     */
    @Transactional
    public T moveDown(T node) {
        return moveNode(node, MoveNodeDirection.DOWN);
    }

    /**
     * Creates a new node in the nested set tree.
     *
     * @param allNodes The list of all nodes in the tree.
     * @param parent   The parent node under which the new node will be created.
     * @return A pair of integers representing the left and right values of the new node.
     */
    @Transactional
    public Pair<Integer, Integer> createNode(List<T> allNodes, T parent) {
        if (parent == null) {
            int maxRight = allNodes.stream()
                .mapToInt(T::getRight)
                .max()
                .orElse(0);
            return Pair.of(maxRight + 1, maxRight + 2);
        } else {
            ID parentId = parent.getId();
            if (parentId == null) {
                throw new IllegalArgumentException("Parent ID cannot be null");
            }

            T parentFromDb = repository.lockNode(parentId)
                .orElseThrow(() -> new IllegalArgumentException("Parent not found: " + parentId));

            int insertPosition = parentFromDb.getRight();
            List<T> nodesToShift = repository.findNodesToShift(insertPosition);

            for (T node : nodesToShift) {
                if (node.getLeft() >= insertPosition) node.setLeft(node.getLeft() + 2);
                if (node.getRight() >= insertPosition) node.setRight(node.getRight() + 2);
            }

            parentFromDb.setRight(parentFromDb.getRight() + 2);

            List<T> combinedList = Stream.concat(Stream.of(parentFromDb), nodesToShift.stream())
                .collect(Collectors.toList());
            saveAllNodes(combinedList);

            return Pair.of(insertPosition, insertPosition + 1);
        }
    }

    /**
     * Closes the gap in the tree after a node is deleted.
     *
     * @param entity   The node that was deleted.
     * @param width    The width of the gap to be closed.
     * @param allNodes The list of all nodes in the tree.
     */
    protected void closeGapInTree(T entity, int width, List<T> allNodes) {
        List<T> updatedNodes = allNodes.stream()
            .filter(node -> node.getLeft() > entity.getRight())
            .peek(node -> {
                node.setLeft(node.getLeft() - width);
                node.setRight(node.getRight() - width);
            })
            .collect(Collectors.toList());

        updatedNodes.addAll(allNodes.stream()
            .filter(node -> node.getRight() > entity.getRight() && node.getLeft() < entity.getRight())
            .peek(node -> node.setRight(node.getRight() - width))
            .toList());
    }

    /**
     * Move a node in the tree.
     *
     * @param node      The node to be moved.
     * @param direction The direction in which the node will be moved (up or down).
     * @return T The updated node.
     */
    @Transactional
    protected T moveNode(T node, MoveNodeDirection direction) {
        ID parentId = node.getParent() != null ? node.getParent().getId() : null;

        Optional<T> siblingOpt;
        if (direction == MoveNodeDirection.UP) {
            siblingOpt = repository.findPrevSibling(parentId, node.getLeft());
        } else {
            siblingOpt = repository.findNextSibling(parentId, node.getRight());
        }

        if (siblingOpt.isEmpty()) return node;

        T sibling = siblingOpt.get();

        int nodeWidth = node.getRight() - node.getLeft() + 1;
        int siblingWidth = sibling.getRight() - sibling.getLeft() + 1;

        List<T> nodeSubtree = repository.findSubtree(node.getLeft(), node.getRight());
        List<T> siblingSubtree = repository.findSubtree(sibling.getLeft(), sibling.getRight());

        for (T n : nodeSubtree) {
            n.setLeft(n.getLeft() + TEMP_OFFSET);
            n.setRight(n.getRight() + TEMP_OFFSET);
        }

        for (T s : siblingSubtree) {
            if (direction == MoveNodeDirection.UP) {
                s.setLeft(s.getLeft() + nodeWidth);
                s.setRight(s.getRight() + nodeWidth);
            } else {
                s.setLeft(s.getLeft() - nodeWidth);
                s.setRight(s.getRight() - nodeWidth);
            }
        }

        for (T n : nodeSubtree) {
            if (direction == MoveNodeDirection.UP) {
                n.setLeft(n.getLeft() - TEMP_OFFSET - siblingWidth);
                n.setRight(n.getRight() - TEMP_OFFSET - siblingWidth);
            } else {
                n.setLeft(n.getLeft() - TEMP_OFFSET + siblingWidth);
                n.setRight(n.getRight() - TEMP_OFFSET + siblingWidth);
            }
        }

        List<T> all = new ArrayList<>();
        all.addAll(nodeSubtree);
        all.addAll(siblingSubtree);
        saveAllNodes(all);

        return node;
    }

    /**
     * Check if a node is a descendant of another node.
     *
     * @param ancestor   The potential ancestor node.
     * @param descendant The potential descendant node.
     * @return True if the descendant is a child of the ancestor, false otherwise.
     */
    protected boolean isDescendant(T ancestor, T descendant) {
        return descendant.getLeft() > ancestor.getLeft() && descendant.getRight() < ancestor.getRight();
    }

    /**
     * Rebuild the tree structure.
     *
     * @param parent      The parent node of the current node being processed.
     * @param allNodes    The list of all nodes in the tree.
     * @param currentLeft The current left value of the node being processed.
     * @return The right value of the node being processed.
     */
    @Transactional
    protected int rebuildTree(T parent, List<T> allNodes, int currentLeft) {
        int left = currentLeft;
        ID parentId = parent == null ? null : parent.getId();

        List<T> children = allNodes.stream()
            .filter(node -> {
                if (parentId == null) {
                    return node.getParent() == null;
                }
                return node.getParent() != null && parentId.equals(node.getParent().getId());
            })
            .sorted(Comparator.comparingInt(INestedSetNode::getLeft))
            .toList();

        for (T child : children) {
            int childLeft = left + 1;
            int right = rebuildTree(child, allNodes, childLeft);
            child.setLeft(childLeft);
            child.setRight(right);
            saveAllNodes(List.of(child));
            left = right;
        }

        return left + 1;
    }

    /**
     * Rebuild the tree structure starting from the root node.
     *
     * @param parent   The root node of the tree.
     * @param allNodes The list of all nodes in the tree.
     * @return The right value of the root node.
     */
    @Transactional
    protected int rebuildTree(T parent, List<T> allNodes) {
        return rebuildTree(parent, allNodes, 0);
    }

    /**
     * Save all nodes in the tree.
     *
     * @param nodes The list of nodes to be saved.
     * @return The list of saved nodes.
     */
    protected List<T> saveAllNodes(List<T> nodes) {
        List<T> savedNodes = repository.saveAll(nodes);
        repository.flush();
        return savedNodes;
    }
}
