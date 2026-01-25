package com.mewebstudio.springboot.jpa.nestedset;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
        // Only process if the parent has changed
        if (hasParentChanged(node, newParent)) {
            // Check for cyclic reference
            if (newParent != null && isDescendant(node, newParent)) {
                throw new IllegalArgumentException("Cannot move category under its own descendant");
            }

            moveNodeToNewParent(node, newParent);
        }

        return repository.save(node);
    }

    /**
     * Check if the parent of a node has changed.
     *
     * @param node      T The node to check.
     * @param newParent T The new parent node.
     * @return boolean True if the parent has changed, false otherwise.
     */
    protected boolean hasParentChanged(T node, T newParent) {
        ID currentParentId = node.getParent() != null ? node.getParent().getId() : null;
        ID newParentId = newParent != null ? newParent.getId() : null;

        if (currentParentId == null && newParentId == null) {
            return false;
        }
        if (currentParentId == null || newParentId == null) {
            return true;
        }
        return !currentParentId.equals(newParentId);
    }

    /**
     * Move a node and its subtree to a new parent.
     *
     * @param node      T The node to be moved.
     * @param newParent T The new parent node.
     */
    @Transactional
    protected void moveNodeToNewParent(T node, T newParent) {
        int oldLeft = node.getLeft();
        int oldRight = node.getRight();
        int subtreeWidth = oldRight - oldLeft + 1;

        // Get all nodes in the subtree (including the node itself)
        List<T> subtreeNodes = repository.findSubtree(oldLeft, oldRight);

        // Step 1: Temporarily move subtree out of the way using a large offset
        moveSubtreeToTempOffset(subtreeNodes);

        // Step 2: Close the gap left by the moved subtree
        closeGapInTree(oldRight, subtreeWidth);

        // Step 3: Calculate a new position for the subtree
        int newLeft = calculateNewPosition(newParent, subtreeWidth);
        int shift = newLeft - oldLeft;

        // Step 4: Move subtree to the new position
        moveSubtreeFromTempToFinalPosition(oldLeft, oldRight, shift);

        // Update the node's parent reference
        node.setParent(newParent);
        // Refresh node's left and right from the saved values
        node.setLeft(oldLeft + shift);
        node.setRight(oldRight + shift);
    }

    /**
     * Calculate the new position for inserting a subtree under a parent.
     *
     * @param parent       T The parent node under which the subtree will be inserted.
     * @param subtreeWidth int The width of the subtree being moved.
     * @return int The new left position for the subtree.
     */
    @Transactional
    protected int calculateNewPosition(T parent, int subtreeWidth) {
        List<T> allNodes = repository.findAllOrderedByLeft().stream()
            .filter(n -> n.getLeft() < TEMP_OFFSET)
            .toList();

        if (parent == null) {
            // Insert at the end as a root node
            int maxRight = allNodes.stream()
                .mapToInt(T::getRight)
                .max()
                .orElse(0);
            return maxRight + 1;
        } else {
            // Re-fetch the parent to get updated values after gap closing
            T parentNode = repository.lockNode(parent.getId())
                .orElseThrow(() -> new EntityNotFoundException("Parent not found: " + parent.getId()));

            int insertAt = parentNode.getRight();

            // Shift nodes to make room for the subtree
            List<T> nodesToShift = repository.findNodesToShift(insertAt - 1).stream()
                .filter(n -> n.getLeft() < TEMP_OFFSET)
                .filter(n -> !n.getId().equals(parentNode.getId()))
                .toList();

            List<T> updatedNodes = new ArrayList<>();
            for (T n : nodesToShift) {
                boolean updated = false;
                if (n.getLeft() >= insertAt) {
                    n.setLeft(n.getLeft() + subtreeWidth);
                    updated = true;
                }
                if (n.getRight() >= insertAt) {
                    n.setRight(n.getRight() + subtreeWidth);
                    updated = true;
                }
                if (updated) {
                    updatedNodes.add(n);
                }
            }

            parentNode.setRight(parentNode.getRight() + subtreeWidth);
            updatedNodes.add(parentNode);
            saveAllNodes(updatedNodes);

            return insertAt;
        }
    }

    /**
     * Deletes a node from the nested set tree.
     *
     * @param node T The node to be deleted.
     */
    @Transactional
    protected void deleteNode(T node) {
        int width = node.getRight() - node.getLeft() + 1;
        int nodeRight = node.getRight();

        // Delete the subtree
        List<T> subtree = repository.findSubtree(node.getLeft(), node.getRight());
        repository.deleteAll(subtree);
        repository.flush();

        // Close the gap in the tree
        closeGapInTree(nodeRight, width);
    }

    /**
     * Closes the gap in the tree after a node is deleted or moved.
     *
     * @param deletedRight int The right value of the deleted/moved node.
     * @param width        int The width of the gap to be closed.
     */
    @Transactional
    protected void closeGapInTree(int deletedRight, int width) {
        List<T> allNodes = repository.findAllOrderedByLeft().stream()
            .filter(n -> n.getLeft() < TEMP_OFFSET)
            .toList();

        List<T> nodesToUpdate = new ArrayList<>();

        // Shift nodes that were to the right of the deleted subtree
        for (T node : allNodes) {
            boolean updated = false;
            if (node.getLeft() > deletedRight) {
                node.setLeft(node.getLeft() - width);
                updated = true;
            }
            if (node.getRight() > deletedRight) {
                node.setRight(node.getRight() - width);
                updated = true;
            }
            if (updated) {
                nodesToUpdate.add(node);
            }
        }

        if (!nodesToUpdate.isEmpty()) {
            saveAllNodes(nodesToUpdate);
        }
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

        int oldNodeLeft = node.getLeft();
        int oldNodeRight = node.getRight();
        int nodeWidth = oldNodeRight - oldNodeLeft + 1;
        int siblingWidth = sibling.get().getRight() - sibling.get().getLeft() + 1;

        // Calculate the shift for node and sibling
        int nodeShift = direction == MoveNodeDirection.UP ? -siblingWidth : siblingWidth;
        int siblingShift = direction == MoveNodeDirection.UP ? nodeWidth : -nodeWidth;

        List<T> nodeSubtree = repository.findSubtree(oldNodeLeft, oldNodeRight);
        List<T> siblingSubtree = repository.findSubtree(sibling.get().getLeft(), sibling.get().getRight());

        // Step 1: Move the node subtree to temp offset
        moveSubtreeToTempOffset(nodeSubtree);

        // Step 2: Move sibling subtree
        for (T n : siblingSubtree) {
            n.setLeft(n.getLeft() + siblingShift);
            n.setRight(n.getRight() + siblingShift);
        }
        saveAllNodes(siblingSubtree);

        // Step 3: Move the node subtree from temp to the final position
        moveSubtreeFromTempToFinalPosition(oldNodeLeft, oldNodeRight, nodeShift);

        // Update the original node reference with new values
        node.setLeft(oldNodeLeft + nodeShift);
        node.setRight(oldNodeRight + nodeShift);

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
     * Rebuild the tree structure (recursive helper).
     *
     * @param parent      T The parent node of the current node being processed.
     * @param allNodes    List The list of all nodes in the tree.
     * @param currentLeft Int The current left value of the node being processed.
     * @param nodesToSave List The list of nodes to be saved after rebuilding.
     * @return Int The right value of the node being processed.
     */
    protected int rebuildTreeRecursive(T parent, List<T> allNodes, int currentLeft, List<T> nodesToSave) {
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
            int right = rebuildTreeRecursive(child, allNodes, childLeft, nodesToSave);
            child.setLeft(childLeft);
            child.setRight(right);
            nodesToSave.add(child);
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
        List<T> nodesToSave = new ArrayList<>();
        int result = rebuildTreeRecursive(parent, allNodes, 0, nodesToSave);
        if (!nodesToSave.isEmpty()) {
            saveAllNodes(nodesToSave);
        }
        return result;
    }

    /**
     * Move a subtree to a temporary offset position.
     *
     * @param subtreeNodes List The list of nodes in the subtree.
     */
    protected void moveSubtreeToTempOffset(List<T> subtreeNodes) {
        for (T n : subtreeNodes) {
            n.setLeft(n.getLeft() + TEMP_OFFSET);
            n.setRight(n.getRight() + TEMP_OFFSET);
        }
        saveAllNodes(subtreeNodes);
    }

    /**
     * Move a subtree from the temporary offset to the final position.
     *
     * @param oldLeft int The original left value of the subtree.
     * @param oldRight int The original right value of the subtree.
     * @param shift int The shift to apply to the subtree.
     */
    protected void moveSubtreeFromTempToFinalPosition(int oldLeft, int oldRight, int shift) {
        List<T> movedSubtreeNodes = repository.findSubtree(oldLeft + TEMP_OFFSET, oldRight + TEMP_OFFSET);
        for (T n : movedSubtreeNodes) {
            n.setLeft(n.getLeft() - TEMP_OFFSET + shift);
            n.setRight(n.getRight() - TEMP_OFFSET + shift);
        }
        saveAllNodes(movedSubtreeNodes);
    }

    /**
     * Save all nodes in the tree.
     *
     * @param nodes List The list of nodes to be saved.
     */
    protected void saveAllNodes(List<T> nodes) {
        repository.saveAll(nodes);
        repository.flush();
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
