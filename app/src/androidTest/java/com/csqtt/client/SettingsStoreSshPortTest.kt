package com.csqtt.client

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsStoreSshPortTest {
    @Test fun vpsDraftKeepsAllCredentialsInItsSlotWithoutChangingWorkersOrVpnCursor() = runBlocking<Unit> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val connections = SettingsStore(context)
        val deploy = SettingsStore(context, deployScope = true)
        val vpnCursor = connections.activeProfile.first()
        connections.saveProfileWorkersPerHash(6, 30)
        deploy.saveActiveProfile(7)
        val first = DeploySettingsSnapshot(profile = 6, serverName = "VPS A", ip = "203.0.113.66",
            sshLogin = "deploy-a", sshPassword = "fixture-password-a", sshPort = "5052",
            serverPeerPort = 46600, serverWebPort = 46602, mainPassword = "TestMainPass1234",
            webLogin = "admin-a", webPassword = "fixture-web-a", sshKeysMode = true)
        deploy.saveDeployProfileDraft(first)
        deploy.saveSshKeys("fixture-private-key-a", "fixture-key-pass-a", "fixture-certificate-a", forProfile = 6)
        deploy.saveDeployProfileDraft(first.copy(profile = 7, serverName = "VPS B", ip = "203.0.113.77",
            sshLogin = "deploy-b", sshPassword = "fixture-password-b", sshPort = "5053",
            serverPeerPort = 47700, serverWebPort = 47702, sshKeysMode = false))
        // Write A while B remains selected: late autosaves must not overwrite B or move cursors.
        deploy.saveDeployProfileDraft(first.copy(webLogin = "updated-admin-a"))
        assertEquals(7, deploy.activeProfile.first())
        assertEquals(vpnCursor, connections.activeProfile.first())
        val a = SettingsStore(context, readOnlyProfile = 6).deploySettingsSnapshot.first { it.isLoaded }
        val b = SettingsStore(context, readOnlyProfile = 7).deploySettingsSnapshot.first { it.isLoaded }
        assertEquals("203.0.113.66", a.ip)
        assertEquals("5052", a.sshPort)
        assertEquals(46600, a.serverPeerPort)
        assertEquals(46602, a.serverWebPort)
        assertEquals("deploy-a", a.sshLogin)
        assertEquals("fixture-password-a", a.sshPassword)
        assertEquals("fixture-private-key-a", a.sshPrivateKey)
        assertEquals("fixture-key-pass-a", a.sshKeyPassphrase)
        assertEquals("fixture-certificate-a", a.sshCertificate)
        assertEquals("updated-admin-a", a.webLogin)
        assertTrue(a.sshKeysMode)
        assertEquals("203.0.113.77", b.ip)
        assertEquals("5053", b.sshPort)
        assertEquals(47700, b.serverPeerPort)
        assertEquals("deploy-b", b.sshLogin)
        assertFalse(b.sshKeysMode)
        assertEquals(30, connections.readProfileConnectionSettings(6).workersPerHash)
        connections.saveProfileConnectionSettings(6, "203.0.113.66", 46600, "fixture-tunnel-password", 30)
        deploy.clearDeployProfile(forProfile = 6)
        val connection = connections.readProfileConnectionSettings(6)
        assertEquals("203.0.113.66", connection.peer)
        assertEquals(46600, connection.serverPeerPort)
        assertEquals("fixture-tunnel-password", connection.connectionPassword)
        val removed = SettingsStore(context, readOnlyProfile = 6).deploySettingsSnapshot.first { it.isLoaded }
        assertEquals("", removed.ip)
        assertEquals("", removed.sshPrivateKey)
        assertEquals("203.0.113.77", SettingsStore(context, readOnlyProfile = 7).deploySettingsSnapshot.first { it.isLoaded }.ip)
    }
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
