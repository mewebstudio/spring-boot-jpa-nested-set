package com.mewebstudio.springboot.jpa.nestedset;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class NestedSetUtilTest {
    private INestedSetNode<Integer> root;
    private INestedSetNode<Integer> child1;
    private INestedSetNode<Integer> child2;

    private INestedSetNodeResponse<Integer> rootResponse;
    private INestedSetNodeResponse<Integer> child1Response;
    private INestedSetNodeResponse<Integer> child2Response;

    @BeforeEach
    void setUp() {
        // Mock nodes
        root = mock(INestedSetNode.class);
        child1 = mock(INestedSetNode.class);
        child2 = mock(INestedSetNode.class);

        when(root.getId()).thenReturn(1);
        when(root.getParent()).thenReturn(null);
        when(root.getLeft()).thenReturn(1);

        when(child1.getId()).thenReturn(2);
        when(child1.getParent()).thenReturn(root);
        when(child1.getLeft()).thenReturn(2);

        when(child2.getId()).thenReturn(3);
        when(child2.getParent()).thenReturn(root);
        when(child2.getLeft()).thenReturn(3);

        // Mock response objects
        rootResponse = mock(INestedSetNodeResponse.class);
        child1Response = mock(INestedSetNodeResponse.class);
        child2Response = mock(INestedSetNodeResponse.class);
    }

    @Test
    void testTreeBuildsCorrectHierarchy() {
        List<INestedSetNode<Integer>> nodes = List.of(root, child1, child2);

        // Response mapping
        Function<INestedSetNode<Integer>, INestedSetNodeResponse<Integer>> converter = node -> {
            Integer id = node.getId();
            if (id.equals(1)) return rootResponse;
            if (id.equals(2)) return child1Response;
            return child2Response;
        };

        // `withChildren` behavior
        when(rootResponse.withChildren(any())).thenReturn(rootResponse);
        when(child1Response.withChildren(any())).thenReturn(child1Response);
        when(child2Response.withChildren(any())).thenReturn(child2Response);

        List<INestedSetNodeResponse<Integer>> result = NestedSetUtil.tree(nodes, converter);

        assertEquals(1, result.size());
        assertSame(rootResponse, result.get(0));

        // `withChildren` call verifications
        verify(rootResponse).withChildren(List.of(child1Response, child2Response));
        verify(child1Response).withChildren(List.of());
        verify(child2Response).withChildren(List.of());
    }

    @Test
    void testTreeReturnsEmptyListIfNodesIsEmpty() {
        List<INestedSetNodeResponse<Integer>> result = NestedSetUtil.tree(List.of(), node -> null);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
