package com.mewebstudio.springboot.jpa.nestedset;

/**
 * Interface representing a node in a nested set tree structure.
 * This interface defines the basic properties and methods for a nested set node.
 *
 * @param <ID> Type of the node identifier.
 */
public interface INestedSetNode<ID> {
    ID getId();

    int getLeft();

    void setLeft(int left);

    int getRight();

    void setRight(int right);

    INestedSetNode<ID> getParent();

    void setParent(INestedSetNode<ID> parent);
}
