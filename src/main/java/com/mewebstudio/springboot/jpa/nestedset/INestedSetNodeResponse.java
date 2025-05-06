package com.mewebstudio.springboot.jpa.nestedset;

import java.util.List;

/**
 * Interface representing a response node in a nested set tree structure.
 * This interface extends the basic properties of a nested set node with additional methods for handling children.
 *
 * @param <ID> Type of the node identifier.
 */
public interface INestedSetNodeResponse<ID> {
    /**
     * Get the left value of the node in the nested set.
     *
     * @return int The left value of the node.
     */
    int getLeft();

    /**
     * Get the right value of the node in the nested set.
     *
     * @return int The right value of the node.
     */
    int getRight();

    /**
     * Get the list of child nodes of this node.
     *
     * @return List The list of child nodes.
     */
    List<INestedSetNodeResponse<ID>> getChildren();

    /**
     * Set the list of child nodes for this node.
     *
     * @param children List The list of child nodes to set.
     * @return INestedSetNodeResponse The current node with updated children.
     */
    INestedSetNodeResponse<ID> withChildren(List<INestedSetNodeResponse<ID>> children);
}
