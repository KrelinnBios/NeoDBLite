package com.krelinnbios.neodblite.util

import com.krelinnbios.neodblite.data.model.NeoUser
import org.junit.Assert.assertEquals
import org.junit.Test

class ProfileUrlTest {
    @Test
    fun keepsAbsoluteProfileUrl() {
        val user = NeoUser(url = "https://neodb.social/users/bios")

        assertEquals("https://neodb.social/users/bios", profileWebUrl(user, "neodb.social"))
    }

    @Test
    fun resolvesRelativeProfileUrlAgainstInstance() {
        val user = NeoUser(url = "/users/bios")

        assertEquals("https://neodb.social/users/bios", profileWebUrl(user, "neodb.social"))
    }

    @Test
    fun addsSchemeToHostPrefixedProfileUrl() {
        val user = NeoUser(url = "neodb.social/users/bios")

        assertEquals("https://neodb.social/users/bios", profileWebUrl(user, "neodb.social"))
    }

    @Test
    fun fallsBackToCurrentUsername() {
        val user = NeoUser(username = "bios")

        assertEquals("https://neodb.social/users/bios", profileWebUrl(user, "https://neodb.social/"))
    }
}
