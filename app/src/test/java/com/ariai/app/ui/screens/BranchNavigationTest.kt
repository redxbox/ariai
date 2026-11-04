package com.ariai.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class BranchNavigationTest {
    @Test
    fun siblingLinksProduceSameStableOrderFromEveryBranch() {
        val branchIds = listOf("message-a", "message-b", "message-c")
        val linksByCurrent = branchIds.associateWith { current -> branchIds.filterNot { it == current } }

        val orders = linksByCurrent.map { (current, siblings) ->
            branchNavigationIds(current, siblings)
        }

        assertEquals(1, orders.distinct().size)
        assertEquals(branchIds, orders.first())
    }

    @Test
    fun addingBranchLinksItAndAllExistingSiblings() {
        val oldBranchIds = listOf("original", "branch-a")
        val originalSiblings = linkedBranchSiblingIds("original", listOf("branch-a"), "branch-b")
        val branchASiblings = linkedBranchSiblingIds("branch-a", listOf("original"), "branch-b")
        val branchBSiblings = linkedBranchSiblingIds("branch-b", oldBranchIds, "branch-b")

        val expected = listOf("original", "branch-a", "branch-b").sorted()
        assertEquals(expected, branchNavigationIds("original", originalSiblings))
        assertEquals(expected, branchNavigationIds("branch-a", branchASiblings))
        assertEquals(expected, branchNavigationIds("branch-b", branchBSiblings))
    }
}
