package com.mewebstudio.springboot.jpa.nestedset;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Abstract service class for managing nested set trees.
 *
 * @param <T>  The type of the nested set node.
 * @param <ID> The type of the identifier for the nested set node.
 */
public abstract class AbstractNestedSetService<T extends INestedSetNode<ID, T>, ID> {
    private static final int TEMP_OFFSET = Integer.MAX_VALUE;

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
     * @param node     T The new node to be created.
     * @return T The created node.
     */
    @Transactional
    protected T createNode(List<T> allNodes, T node) {
        Pair<Integer, Integer> gap = getNodeGap(allNodes, node.getParent());
        node.setLeft(gap.first());
        node.setRight(gap.second());
        return repository.save(node);
    }

    /**
     * Creates a new node in the nested set tree.
     *
     * @param node T The new node to be created.
     * @return T The created node.
     */
    @Transactional
    protected T createNode(T node) {
        return createNode(repository.findAllOrderedByLeft(), node);
    }

    /**
     * Get the gap for inserting a new node in the nested set tree.
     *
     * @param allNodes The list of all nodes in the tree.
     * @param parent   T The parent node under which the new node will be created.
     * @return A pair of integers representing the left and right values for the new node.
     */
    @Transactional
    protected Pair<Integer, Integer> getNodeGap(List<T> allNodes, INestedSetNode<ID, T> parent) {
        if (parent == null) {
            int maxRight = allNodes.stream()
                .mapToInt(T::getRight)
                .max()
                .orElse(0);
            return new Pair<>(maxRight + 1, maxRight + 2);
        } else {
            ID parentId = parent.getId();
            T parentNode = repository.lockNode(parentId).orElseThrow(() ->
                new EntityNotFoundException("Parent node not found with id: " + parentId));

            int insertAt = parentNode.getRight();
            List<T> shiftedNodes = repository.findNodesToShift(insertAt);
            for (T node : shiftedNodes) {
                if (node.getLeft() >= insertAt) node.setLeft(node.getLeft() + 2);
                if (node.getRight() >= insertAt) node.setRight(node.getRight() + 2);
            }

            parentNode.setRight(parentNode.getRight() + 2);
            saveAllNodes(mergeList(Collections.singletonList(parentNode), shiftedNodes));

            return new Pair<>(insertAt, insertAt + 1);
        }
    }

    /**
     * Update a node in the nested set tree.
     *
     * @param node      T The node to be updated.
     * @param newParent T The new parent node under which the node will be moved.
     * @return T The updated node.
     */
    @Transactional
    protected T updateNode(T node, T newParent) {
        if (newParent != null && isDescendant(node, newParent)) {
            throw new IllegalArgumentException("Cannot move category under its own descendant");
        }

        int distance = node.getRight() - node.getLeft() + 1;
        List<T> allCategories = repository.findAllOrderedByLeft();
        closeGapInTree(node, distance, allCategories);

        Pair<Integer, Integer> nodePositions = getNodeGap(allCategories, newParent);
        node.setParent(newParent);
        node.setLeft(nodePositions.first());
        node.setRight(nodePositions.second());

        return repository.save(node);
    }

    /**
     * Deletes a node from the nested set tree.
     *
     * @param node T The node to be deleted.
     */
    @Transactional
    protected void deleteNode(T node) {
        int width = node.getRight() - node.getLeft() + 1;
        List<T> subtree = repository.findSubtree(node.getLeft(), node.getRight());
        repository.deleteAll(subtree);
        closeGapInTree(node, width, repository.findAllOrderedByLeft());
    }

    /**
     * Closes the gap in the tree after a node is deleted.
     *
     * @param entity   T The node that was deleted.
     * @param width    int The width of the gap to be closed.
     * @param allNodes List The list of all nodes in the tree.
     */
    protected void closeGapInTree(T entity, int width, List<T> allNodes) {
        List<T> updatedNodes = allNodes.stream()
            .filter(n -> n.getLeft() > entity.getRight())
            .peek(n -> {
                n.setLeft(n.getLeft() - width);
                n.setRight(n.getRight() - width);
            })
            .collect(Collectors.toList());

        updatedNodes.addAll(allNodes.stream()
            .filter(n -> n.getRight() > entity.getRight() && n.getLeft() < entity.getRight())
            .peek(n -> n.setRight(n.getRight() - width))
            .toList());
    }

    /**
     * Move a node in the tree.
     *
     * @param node      T The node to be moved.
     * @param direction MoveNodeDirection The direction in which the node will be moved (up or down).
     * @return T The updated node.
     */
    @Transactional
    protected T moveNode(T node, MoveNodeDirection direction) {
        ID parentId = node.getParent() != null ? node.getParent().getId() : null;
        Optional<T> sibling = direction == MoveNodeDirection.UP ?
            repository.findPrevSibling(parentId, node.getLeft()) :
            repository.findNextSibling(parentId, node.getRight());

        if (sibling.isEmpty()) return node;

        int nodeWidth = node.getRight() - node.getLeft() + 1;
        int siblingWidth = sibling.get().getRight() - sibling.get().getLeft() + 1;

        List<T> nodeSubtree = repository.findSubtree(node.getLeft(), node.getRight());
        List<T> siblingSubtree = repository.findSubtree(sibling.get().getLeft(), sibling.get().getRight());

        nodeSubtree.forEach(n -> {
            n.setLeft(n.getLeft() + TEMP_OFFSET);
            n.setRight(n.getRight() + TEMP_OFFSET);
        });

        for (T n : siblingSubtree) {
            int offset = direction == MoveNodeDirection.UP ? nodeWidth : -nodeWidth;
            n.setLeft(n.getLeft() + offset);
            n.setRight(n.getRight() + offset);
        }

        for (T n : nodeSubtree) {
            int offset = direction == MoveNodeDirection.UP ? -TEMP_OFFSET - siblingWidth : -TEMP_OFFSET + siblingWidth;
            n.setLeft(n.getLeft() + offset);
            n.setRight(n.getRight() + offset);
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
     * @param ancestor   T The potential ancestor node.
     * @param descendant T The potential descendant node.
     * @return True if the descendant is a child of the ancestor, false otherwise.
     */
    protected boolean isDescendant(T ancestor, T descendant) {
        return descendant.getLeft() > ancestor.getLeft() && descendant.getRight() < ancestor.getRight();
    }

    /**
     * Rebuild the tree structure.
     *
     * @param parent      T The parent node of the current node being processed.
     * @param allNodes    List The list of all nodes in the tree.
     * @param currentLeft Int The current left value of the node being processed.
     * @return Int The right value of the node being processed.
     */
    @Transactional
    protected int rebuildTree(T parent, List<T> allNodes, int currentLeft) {
        int left = currentLeft;
        ID parentId = parent != null ? parent.getId() : null;

        List<T> children = allNodes.stream()
            .filter(node -> {
                if (parentId == null) return node.getParent() == null;
                return node.getParent() != null && parentId.equals(node.getParent().getId());
            })
            .sorted(Comparator.comparingInt(T::getLeft))
            .toList();

        for (T child : children) {
            int childLeft = left + 1;
            int right = rebuildTree(child, allNodes, childLeft);
            child.setLeft(childLeft);
            child.setRight(right);
            saveAllNodes(Collections.singletonList(child));
            left = right;
        }

        return left + 1;
    }

    /**
     * Rebuild the tree structure starting from the root node.
     *
     * @param parent   T The root node of the tree.
     * @param allNodes List The list of all nodes in the tree.
     * @return int The right value of the root node.
     */
    @Transactional
    protected int rebuildTree(T parent, List<T> allNodes) {
        return rebuildTree(parent, allNodes, 0);
    }

    /**
     * Save all nodes in the tree.
     *
     * @param nodes List The list of nodes to be saved.
     * @return List The list of saved nodes.
     */
    protected List<T> saveAllNodes(List<T> nodes) {
        List<T> savedNodes = repository.saveAll(nodes);
        repository.flush();
        return savedNodes;
    }

    /**
     * Merge two lists into one.
     *
     * @param list1 List The first list.
     * @param list2 List The second list.
     * @return List The merged list.
     */
    private List<T> mergeList(List<T> list1, List<T> list2) {
        List<T> merged = new ArrayList<>(list1);
        merged.addAll(list2);
        return merged;
    }
}
