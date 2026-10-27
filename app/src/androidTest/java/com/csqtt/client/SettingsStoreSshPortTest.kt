package com.csqtt.client

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsStoreSshPortTest {
    @Test
    fun savingAutomaticServicePortsPreservesSshPort() = runBlocking {
        val store = SettingsStore(InstrumentationRegistry.getInstrumentation().targetContext)
        val before = store.deploySettingsSnapshot.first { it.isLoaded }
        try {
            store.saveManualPortsEnabled(true)
            store.savePorts(47000, 47002, "5052")
            // V2 saves the connection first, then the service-port mode.
            store.saveManualPortsEnabled(false)

            val saved = store.deploySettingsSnapshot.first { it.isLoaded }
            assertEquals("5052", saved.sshPort)
            assertFalse(saved.manualPortsEnabled)
            assertEquals(CsqttConstants.Network.DEFAULT_SERVER_PEER_PORT, saved.serverPeerPort)
            assertEquals(CsqttConstants.Network.DEFAULT_SERVER_WEB_PORT, saved.serverWebPort)
        } finally {
            store.saveManualPortsEnabled(before.manualPortsEnabled)
            store.savePorts(before.serverPeerPort, before.serverWebPort, before.sshPort)
        }
    }
}
