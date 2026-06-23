package com.photocleaner.feature.duplicate.graph

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SimilarityGraphTest {

    private lateinit var graph: SimilarityGraph

    @Before
    fun setup() {
        graph = SimilarityGraph()
    }

    @Test
    fun testInitializeAndNoEdges() {
        graph.initialize(listOf(1L, 2L, 3L))
        val components = graph.getConnectedComponents()
        assertTrue("No edges should result in no components", components.isEmpty())
    }

    @Test
    fun testAddSingleEdge() {
        graph.initialize(listOf(1L, 2L, 3L))
        val merged = graph.addEdge(1L, 2L, similarity = 0.9f, threshold = 0.5f)
        assertTrue("should merge on high similarity", merged)

        val components = graph.getConnectedComponents()
        assertEquals("expect 1 component", 1, components.size)
        val component = components.values.first()
        assertTrue(component.contains(1L))
        assertTrue(component.contains(2L))
    }

    @Test
    fun testAddEdgeBelowThreshold() {
        graph.initialize(listOf(1L, 2L))
        val merged = graph.addEdge(1L, 2L, similarity = 0.3f, threshold = 0.5f)
        assertFalse("should NOT merge when similarity < threshold", merged)

        val components = graph.getConnectedComponents()
        assertTrue("no components when below threshold", components.isEmpty())
    }

    @Test
    fun testMultipleEdgesFormSingleComponent() {
        graph.initialize(listOf(1L, 2L, 3L, 4L))
        graph.addEdge(1L, 2L, similarity = 0.9f, threshold = 0.5f)
        graph.addEdge(2L, 3L, similarity = 0.8f, threshold = 0.5f)
        graph.addEdge(3L, 4L, similarity = 0.7f, threshold = 0.5f)

        val components = graph.getConnectedComponents()
        assertEquals(1, components.size)
        val ids = components.values.first()
        assertEquals(4, ids.size)
        assertTrue(ids.containsAll(listOf(1L, 2L, 3L, 4L)))
    }

    @Test
    fun testTwoSeparateComponents() {
        graph.initialize(listOf(1L, 2L, 3L, 4L, 5L))
        // Component A: {1, 2}
        graph.addEdge(1L, 2L, similarity = 0.9f, threshold = 0.5f)
        // Component B: {3, 4}
        graph.addEdge(3L, 4L, similarity = 0.9f, threshold = 0.5f)
        // 5 is isolated

        val components = graph.getConnectedComponents()
        assertEquals("expect 2 components (>=2 size)", 2, components.size)

        val allIds = components.values.flatMap { it }.toSet()
        assertEquals(setOf(1L, 2L, 3L, 4L), allIds)
    }

    @Test
    fun testClearResetsState() {
        graph.initialize(listOf(1L, 2L))
        graph.addEdge(1L, 2L, similarity = 0.9f, threshold = 0.5f)
        assertEquals(1, graph.getConnectedComponents().size)

        graph.clear()
        val components = graph.getConnectedComponents()
        assertTrue("After clear, components should be empty", components.isEmpty())
    }

    @Test
    fun testAddEdgeWithUnknownIdReturnsFalse() {
        graph.initialize(listOf(1L, 2L))
        val merged = graph.addEdge(1L, 999L, similarity = 0.9f, threshold = 0.5f)
        assertFalse("unknown ID should return false", merged)
    }
}
