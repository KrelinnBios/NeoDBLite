package com.krelinnbios.neodblite.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthStoreTest {
    @Test
    fun normalizeHostStripsSchemeAndPath() {
        assertEquals("neodb.social", AuthStore.normalizeHost("https://neodb.social/"))
        assertEquals("neodb.social", AuthStore.normalizeHost("http://neodb.social"))
        assertEquals("neodb.social", AuthStore.normalizeHost("neodb.social/users/me"))
        assertEquals("neodb.social", AuthStore.normalizeHost("  neodb.social  "))
    }

    @Test
    fun normalizeHostFallsBackToDefaultWhenBlank() {
        assertEquals(AuthStore.DEFAULT_HOST, AuthStore.normalizeHost(""))
        assertEquals(AuthStore.DEFAULT_HOST, AuthStore.normalizeHost("   "))
    }
}
