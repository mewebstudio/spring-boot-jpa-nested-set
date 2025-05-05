package com.mewebstudio.springboot.jpa.nestedset;

import java.util.List;

/**
 * Interface representing a response node in a nested set tree structure.
 * This interface extends the basic properties of a nested set node with additional methods for handling children.
 *
 * @param <ID> Type of the node identifier.
 */
public interface INestedSetNodeResponse<ID> {
    int getLeft();

    int getRight();

    List<INestedSetNodeResponse<ID>> getChildren();

    INestedSetNodeResponse<ID> withChildren(List<INestedSetNodeResponse<ID>> children);
}
