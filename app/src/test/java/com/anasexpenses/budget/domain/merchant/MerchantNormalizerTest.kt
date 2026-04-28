package com.anasexpenses.budget.domain.merchant

import org.junit.Assert.assertEquals
import org.junit.Test

class MerchantNormalizerTest {
    @Test
    fun merchantToken_stripsNonAlphanumeric() {
        assertEquals("talabat", MerchantNormalizer.merchantToken("TALABAT.COM"))
        assertEquals("talabat", MerchantNormalizer.merchantToken("Talabat"))
    }
}
