package com.makd.afinity.shared.viewrr

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionStoreTest {

    @Test
    fun newStore_isLoggedOutWithNoToken() {
        val store = SessionStore()

        assertFalse(store.isLoggedIn.value)
        assertNull(store.token)
    }

    @Test
    fun setToken_logsInAndStoresToken() {
        val store = SessionStore()

        store.setToken("t")

        assertTrue(store.isLoggedIn.value)
        assertEquals("t", store.token)
    }

    @Test
    fun setTokenNull_logsOut() {
        val store = SessionStore()
        store.setToken("t")

        store.setToken(null)

        assertFalse(store.isLoggedIn.value)
        assertNull(store.token)
    }

    @Test
    fun clear_logsOut() {
        val store = SessionStore()
        store.setToken("t")

        store.clear()

        assertFalse(store.isLoggedIn.value)
        assertNull(store.token)
    }

    @Test
    fun setBlankToken_isNotLoggedIn() {
        val store = SessionStore()

        store.setToken("")

        assertFalse(store.isLoggedIn.value)
        assertEquals("", store.token)
    }
}
