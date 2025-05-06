package com.mewebstudio.springboot.jpa.nestedset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@DisplayName("Test for NestedSetUtil class.")
class NestedSetUtilTest {
    private TestNode root;
    private TestNode child1;
    private TestNode child2;

    private INestedSetNodeResponse<Integer> rootResponse;
    private INestedSetNodeResponse<Integer> child1Response;
    private INestedSetNodeResponse<Integer> child2Response;

    @BeforeEach
    void setUp() {
        root = new TestNode(1, 1, 6, null);
        child1 = new TestNode(2, 2, 3, root);
        child2 = new TestNode(3, 4, 5, root);

        rootResponse = mock(INestedSetNodeResponse.class);
        child1Response = mock(INestedSetNodeResponse.class);
        child2Response = mock(INestedSetNodeResponse.class);

        when(rootResponse.withChildren(anyList())).thenReturn(rootResponse);
        when(child1Response.withChildren(anyList())).thenReturn(child1Response);
        when(child2Response.withChildren(anyList())).thenReturn(child2Response);

        when(rootResponse.getLeft()).thenReturn(1);
        when(rootResponse.getRight()).thenReturn(6);
        when(child1Response.getLeft()).thenReturn(2);
        when(child1Response.getRight()).thenReturn(3);
        when(child2Response.getLeft()).thenReturn(4);
        when(child2Response.getRight()).thenReturn(5);
    }

    @Test
    void testTreeBuildsCorrectHierarchy() {
        List<TestNode> nodes = List.of(root, child1, child2);

        Function<TestNode, INestedSetNodeResponse<Integer>> converter = node -> {
            if (node.getId() == 1) return rootResponse;
            if (node.getId() == 2) return child1Response;
            if (node.getId() == 3) return child2Response;
            throw new IllegalArgumentException("Unknown node ID: " + node.getId());
        };

        List<INestedSetNodeResponse<Integer>> result = NestedSetUtil.<Integer, TestNode, INestedSetNodeResponse<Integer>>tree(nodes, converter);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertSame(rootResponse, result.get(0));

        verify(rootResponse).withChildren(argThat(list ->
            list.size() == 2 &&
            list.contains(child1Response) &&
            list.contains(child2Response)
        ));
        verify(child1Response).withChildren(Collections.emptyList());
        verify(child2Response).withChildren(Collections.emptyList());
    }
}

class TestNode implements INestedSetNode<Integer, TestNode> {
    private final Integer id;
    private int left;
    private int right;
    private TestNode parent;

    public TestNode(Integer id, int left, int right, TestNode parent) {
        this.id = id;
        this.left = left;
        this.right = right;
        this.parent = parent;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    public int getLeft() {
        return left;
    }

    @Override
    public void setLeft(int left) {
        this.left = left;
    }

    @Override
    public int getRight() {
        return right;
    }

    @Override
    public void setRight(int right) {
        this.right = right;
    }

    @Override
    public TestNode getParent() {
        return parent;
    }

    @Override
    public void setParent(INestedSetNode<Integer, TestNode> parent) {
        this.parent = (TestNode) parent;
    }
}
