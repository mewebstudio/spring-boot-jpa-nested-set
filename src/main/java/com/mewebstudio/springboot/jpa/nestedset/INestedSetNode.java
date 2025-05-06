package com.mewebstudio.springboot.jpa.nestedset;

/**
 * Interface representing a node in a nested set tree structure.
 * This interface defines the basic properties and methods for a nested set node.
 *
 * @param <ID> Type of the node identifier.
 * @param <T>  Type of the nested set node.
 */
public interface INestedSetNode<ID, T extends INestedSetNode<ID, T>> {
    /**
     * Get the identifier of the node.
     *
     * @return ID The identifier of the node.
     */
    ID getId();

    /**
     * Get the left value of the node in the nested set.
     *
     * @return int The left value of the node.
     */
    int getLeft();

    /**
     * Set the left value of the node in the nested set.
     *
     * @param left The left value to set.
     */
    void setLeft(int left);

    /**
     * Get the right value of the node in the nested set.
     *
     * @return int The right value of the node.
     */
    int getRight();

    /**
     * Set the right value of the node in the nested set.
     *
     * @param right The right value to set.
     */
    void setRight(int right);

    /**
     * Get the parent node of this node.
     *
     * @return INestedSetNode The parent node.
     */
    INestedSetNode<ID, T> getParent();

    /**
     * Set the parent node of this node.
     *
     * @param parent The parent node to set.
     */
    void setParent(INestedSetNode<ID, T> parent);
}
