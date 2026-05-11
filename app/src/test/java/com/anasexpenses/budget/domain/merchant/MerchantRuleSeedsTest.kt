package com.anasexpenses.budget.domain.merchant

import com.anasexpenses.budget.domain.category.DefaultCategorySeeds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MerchantRuleSeedsTest {

    private val allowedNames = DefaultCategorySeeds.rows.map { it.name }.toSet()

    @Test
    fun pairs_tokensUnique_nonBlank_targetsAreDefaultCategoryNames() {
        val pairs = MerchantRuleSeeds.pairs
        assertTrue(pairs.isNotEmpty())
        assertEquals(pairs.size, pairs.map { it.first }.distinct().size)
        for ((token, cat) in pairs) {
            assertTrue(token.isNotBlank())
            assertTrue("target $cat", cat in allowedNames)
        }
    }
}
