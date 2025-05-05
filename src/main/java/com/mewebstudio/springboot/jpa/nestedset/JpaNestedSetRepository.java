package com.mewebstudio.springboot.jpa.nestedset;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing nested set nodes.
 * This interface extends JpaRepository and provides additional methods for nested set operations.
 *
 * @param <T>  Type of the nested set node.
 * @param <ID> Type of the identifier for the nested set node.
 */
@NoRepositoryBean
public interface JpaNestedSetRepository<T extends INestedSetNode<ID>, ID> extends JpaRepository<T, ID> {
    /**
     * Find all nodes in the tree, ordered by their left value.
     * This method retrieves all nodes in the tree structure.
     */
    @Query("SELECT e FROM #{#entityName} e ORDER BY e.left")
    List<T> findAllOrderedByLeft();

    /**
     * Find all root nodes in the tree.
     * This method retrieves nodes where the parent is null.
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.parent IS NULL ORDER BY e.left")
    List<T> findRootNodes();

    /**
     * Find all leaf nodes in the tree.
     * Retrieves nodes where right = left + 1, meaning they have no children and are leaf nodes.
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.left + 1 = e.right")
    List<T> findLeafNodes();

    /**
     * Find previous sibling of a node by its parentId and left value.
     */
    @Query("""
            SELECT e FROM #{#entityName} e
            WHERE e.right < :left
              AND (
                (:parentId IS NULL AND e.parent IS NULL)
                OR (e.parent.id = :parentId)
              )
            ORDER BY e.left DESC LIMIT 1
        """)
    Optional<T> findPrevSibling(@Param("parentId") ID parentId, @Param("left") int left);

    /**
     * Find next sibling of a node by its parentId and left value.
     */
    @Query("""
            SELECT e FROM #{#entityName} e
            WHERE e.left > :right
              AND (
                (:parentId IS NULL AND e.parent IS NULL)
                OR (e.parent.id = :parentId)
              )
            ORDER BY e.left ASC LIMIT 1
        """)
    Optional<T> findNextSibling(@Param("parentId") ID parentId, @Param("right") int right);

    /**
     * Find all children of a given parent node.
     * This method retrieves nodes where the parent ID matches the provided parentId.
     */
    @Query("""
            SELECT e FROM #{#entityName} e
            WHERE (:parentId IS NULL AND e.parent IS NULL)
               OR (:parentId IS NOT NULL AND e.parent.id = :parentId)
            ORDER BY e.left
        """)
    List<T> findChildren(@Param("parentId") ID parentId);

    /**
     * Find all siblings of a given node.
     * This method retrieves nodes where the parent ID matches the provided parentId
     * and the node ID is not equal to the provided selfId.
     * This is used to find nodes that share the same parent.
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.parent.id = :parentId AND e.id <> :selfId ORDER BY e.left")
    List<T> findSiblings(@Param("parentId") ID parentId, @Param("selfId") ID selfId);

    /**
     * Find all ancestors of a given node.
     * This method retrieves nodes where the left value is less than the provided left value
     * and the right value is greater than the provided right value.
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.left < :left AND e.right > :right ORDER BY e.left DESC")
    List<T> findAncestors(@Param("left") int left, @Param("right") int right);

    /**
     * Find all descendants of a given node.
     * This method retrieves nodes where the left value is greater than the provided left value
     * and the right value is less than the provided right value.
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.left > :left AND e.right < :right ORDER BY e.left")
    List<T> findDescendants(@Param("left") int left, @Param("right") int right);

    /**
     * Find all subtrees of a regular node, including its subtrees.
     * This method retrieves nodes where the left value is greater than or equal to the provided left value
     * and the right value is less than or equal to the provided right value.
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.left >= :left AND e.right <= :right ORDER BY e.left ASC")
    List<T> findSubtree(@Param("left") int left, @Param("right") int right);

    /**
     * Find all the nodes (top level nodes) that cover the given range.
     * This method retrieves nodes where the left value is less than or equal to the provided left value
     * and the right value is greater than or equal to the provided right value.
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.left <= :left AND e.right >= :right ORDER BY e.left")
    List<T> findContaining(@Param("left") int left, @Param("right") int right);

    /**
     * Find all the node that exactly matches the given left and right values.
     * This method retrieves nodes where the left value is equal to the provided left value
     * and the right value is equal to the provided right value.
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.left = :left AND e.right = :right ORDER BY e.left")
    List<T> findExact(@Param("left") int left, @Param("right") int right);

    /**
     * Retrieves all ancestor nodes of a given node based on its left and right values, excluding the node itself.
     * This method retrieves nodes where the left value is less than the provided left value
     * and the right value is greater than the provided right value.
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.left < :left AND e.right > :right ORDER BY e.left")
    List<T> findExactExcluding(@Param("left") int left, @Param("right") int right);

    /**
     * Retrieves nodes with a right value greater than the specified value.
     * Used when shifting nodes during insertion or deletion to maintain nested set integrity.
     * This method retrieves nodes where the right value is greater than the provided right value.
     * This is used to find nodes that need to be shifted when inserting a new node.
     */
    @Query("SELECT c FROM #{#entityName} c WHERE c.right > :right")
    List<T> findNodesToShift(@Param("right") int right);

    /**
     * Find a node by its left value.
     * This method retrieves a single node where the left value matches the provided left value.
     */
    Optional<T> findByLeft(int left);

    /**
     * Find a node by its right value.
     * This method retrieves a single node where the right value matches the provided right value.
     */
    Optional<T> findByRight(int right);

    /**
     * Find a node by its left and right values.
     * This method retrieves a single node where the left and right values match the provided values.
     */
    Optional<T> findByLeftAndRight(int left, int right);

    /**
     * Find all nodes that are children of a given parent node.
     * This method retrieves nodes where the parent ID matches the provided parentId.
     */
    @Query("SELECT c FROM #{#entityName} c WHERE c.parent.id = :parentId ORDER BY c.left")
    List<T> findByParentId(@Param("parentId") ID parentId);

    /**
     * Find all nodes with left value between the specified range.
     * This is useful when moving nodes within the nested set.
     */
    @Query("SELECT e FROM #{#entityName} e WHERE e.left BETWEEN :left AND :right ORDER BY e.left")
    List<T> findByLeftBetween(@Param("left") int left, @Param("right") int right);

    /**
     * Lock a node by its ID.
     * Prevents race conditions by locking a node with a pessimistic write lock.
     * Especially useful for nested set manipulations.
     * This method retrieves a single node where the ID matches the provided ID and locks it for writing.
     */
    @Query("SELECT c FROM #{#entityName} c WHERE c.id = :id")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<T> lockNode(@Param("id") ID id);
}
