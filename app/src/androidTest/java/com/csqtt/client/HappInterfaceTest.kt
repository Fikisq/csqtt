package com.csqtt.client

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.csqtt.client.ui.v2.navigation.CsqttV2Root
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import java.io.File
import com.csqtt.client.ui.legacy.utils.formatCsqttLink
import com.csqtt.client.ui.v2.screens.connection.saveImportedLink
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Navigation and persistent native settings only. Never starts SSH, deploy, or VPN. */
@RunWith(AndroidJUnit4::class)
class HappInterfaceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var store: SettingsStore

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(compose.activity.getExternalFilesDir(null), name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Before fun showApp() {
        store = SettingsStore(compose.activity)
        runBlocking {
            store.saveUiOwnerMode(false)
            store.saveThemeMode("dark")
            store.saveUpdateCheckIntervalHours(UPDATE_CHECK_NEVER)
            store.saveActiveProfile(0)
            store.saveProfileConnectionSettings(0, "203.0.113.10", 46000, "DemoPassword1234", 20)
        }
        compose.setContent { CsqttV2Root(store) }
        compose.waitForIdle()
    }

    @Test fun participantCanOpenTunnelAndHelpWithoutDeployTab() {
        compose.onNodeWithText("Деплой").assertDoesNotExist()
        compose.onNodeWithText("Туннель").performClick()
        compose.onNodeWithText("Пароль туннеля").assertIsDisplayed()
        compose.onNodeWithText("Потоки").assertExists()
        compose.onNodeWithContentDescription("Подсказка: Режим SRTP").performClick()
        compose.onNodeWithText("Понятно").assertIsDisplayed().performClick()
        screenshot("happ-tunnel.png")
    }

    @Test fun ownerModeRevealsRealDeployAndPersistsSelection() {
        compose.onNodeWithContentDescription("Настройки").performClick()
        compose.onNodeWithText("Участник").performClick()
        compose.onNodeWithText("Владелец").performClick()
        compose.onNodeWithContentDescription("Назад").performClick()
        compose.onNodeWithText("Деплой").performClick()
        compose.onNodeWithText("IP сервера").assertIsDisplayed()
        compose.onNodeWithText("Порт SSH").assertIsDisplayed()
        compose.onNodeWithText("Установка в Docker").assertExists()
        compose.onNodeWithText("Проверить VPS").assertDoesNotExist()
        compose.onNodeWithText("Логин веб-панели").assertIsDisplayed()
        compose.onNodeWithText("Пароль веб-панели").assertIsDisplayed()
        compose.onNodeWithContentDescription("Сгенерировать пароль туннеля").performClick()
        val deploy = SettingsStore(compose.activity, deployScope = true)
        compose.waitUntil(5000) { runBlocking { deploy.deploySettingsSnapshot.first { it.isLoaded }.mainPassword.matches(Regex("[A-Za-z0-9]{16}")) } }
        compose.onNodeWithText("Удалить с сервера").assertExists()
        compose.runOnIdle { runBlocking { check(store.uiOwnerMode.first()) } }
        screenshot("happ-deploy.png")
    }

    @Test fun selectingVpsAndSyncingDeployPasswordDoNotOverwriteOtherProfile() {
        val deploy = SettingsStore(compose.activity, deployScope = true)
        runBlocking {
            deploy.saveActiveProfile(2)
            deploy.saveDeploy("203.0.113.20")
            check(store.activeProfile.first() == 0)
            deploy.saveDeployedServer(2, "203.0.113.20", 46100, 46102)
            deploy.saveConnectionPassword("NewPassword12345", forProfile = 2)
            check(store.readProfileConnectionSettings(0).connectionPassword == "DemoPassword1234")
            val installed = store.readProfileConnectionSettings(2)
            check(installed.peer == "203.0.113.20" && installed.serverPeerPort == 46100)
            check(installed.connectionPassword == "NewPassword12345")
            check(store.activeProfile.first() == 0)
        }
    }

    @Test fun selectingImportedLinkShowsItsTunnelDataAndPreservesThePreviousProfile() {
        val hashes = (1..6).map { "test-hash-number-$it-123456" }
        runBlocking {
            val link = formatCsqttLink("203.0.113.41", 46200, "LinkPassword123", hashes = hashes, power = 33)
            store.saveImportedLink(1, link, "Сервер из ссылки")
        }
        compose.waitUntil(5000) { compose.onAllNodesWithText("Сервер из ссылки").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Сервер из ссылки").performClick()
        compose.onNodeWithText("Туннель").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("203.0.113.41").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("203.0.113.41").assertIsDisplayed()
        compose.onNodeWithText("46200").assertIsDisplayed()
        runBlocking {
            check(store.activeProfile.first() == 1)
            val saved = store.readProfileConnectionSettings(1)
            check(saved.connectionPassword == "LinkPassword123")
            check(saved.workersPerHash == 30)
            check(store.vkHashes.first() == hashes.joinToString(","))
            check(store.vkHashMode.first() == "manual")
        }
        compose.onNodeWithText("Подкл.").performClick()
        compose.onNodeWithText("Профиль 1").performClick()
        compose.onNodeWithText("Туннель").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("203.0.113.10").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("203.0.113.10").assertIsDisplayed()
        runBlocking { check(store.readProfileConnectionSettings(0).connectionPassword == "DemoPassword1234") }
    }

    @Test fun selectingTokenLinkUsesItsOwnVkAccountInTunnel() {
        runBlocking {
            store.saveImportedLink(3, formatCsqttLink("203.0.113.43", 46203, "TokenLinkPassword", token = "test-token-never-sent", power = 25), "Ссылка с токеном")
        }
        compose.waitUntil(5000) { compose.onAllNodesWithText("Ссылка с токеном").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Ссылка с токеном").performClick()
        compose.onNodeWithText("Туннель").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Аккаунт VK подключён").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Аккаунт VK подключён").assertIsDisplayed()
        compose.onNodeWithText("203.0.113.43").assertIsDisplayed()
        runBlocking {
            check(store.vkHashMode.first() == "auto_api")
            check(store.vkAccessToken.first() == "test-token-never-sent")
            check(store.readProfileConnectionSettings(3).connectionPassword == "TokenLinkPassword")
        }
    }

    @Test fun manualHashesExposeAllSixFieldsAndSaveAnEditedValue() {
        val hashes = (1..6).map { "manual-hash-$it-1234567890" }
        runBlocking { store.saveVkHashMode("manual"); store.saveVkHashesOnly(hashes.joinToString(",")) }
        compose.onNodeWithText("Туннель").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Настроить хеши").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Настроить хеши").performClick()
        hashes.forEach { compose.onNodeWithText(it).assertIsDisplayed() }
        compose.onNodeWithText("Сохранить").assertIsDisplayed()
        screenshot("happ-manual-hashes.png")
        compose.onNodeWithText(hashes.first()).performTextReplacement("edited-hash-1234567890")
        compose.onNodeWithText("Сохранить").performClick()
        compose.waitUntil(5000) { runBlocking { store.vkHashes.first().startsWith("edited-hash-1234567890,") } }
        runBlocking { check(store.vkHashes.first().split(',').drop(1) == hashes.drop(1)) }
    }

    @Test fun logsCanFilterErrorsAndRemoveSeparatorLines() {
        compose.runOnIdle {
            TunnelManager.logs.clear()
            TunnelManager.logs.add(LogEntry("deploy_fixture_error", "[ERR] Тест: SSH отклонён", isError = true, level = LogLevel.ERR))
            TunnelManager.logs.add(LogEntry("fixture_ok", "[OK] Тест: соединение готово", level = LogLevel.OK))
            TunnelManager.logs.add(LogEntry("fixture_separator", "────────────", level = LogLevel.LOG))
        }
        compose.onNodeWithText("Логи").performClick()
        compose.onNodeWithText("Все уровни ▾").performClick()
        compose.onNodeWithText("Ошибки").performClick()
        compose.onNodeWithText("Тест: SSH отклонён").assertIsDisplayed()
        compose.onNodeWithText("Тест: соединение готово").assertDoesNotExist()
        compose.onNodeWithText("────────────").assertDoesNotExist()
        screenshot("ui-logs-review3.png")
        compose.onNodeWithText("Ошибки ▾").performClick()
        compose.onNodeWithText("Все уровни").performClick()
        compose.onNodeWithText("Тест: соединение готово").assertIsDisplayed()
    }

    @Test fun vpsEditorShowsPortsAndFormatHintsWithoutConnectionWorkers() {
        compose.onNodeWithContentDescription("Настройки").performClick()
        compose.onNodeWithText("Участник").performClick()
        compose.onNodeWithText("Владелец").performClick()
        compose.onNodeWithContentDescription("Назад").performClick()
        compose.onNodeWithText("Деплой").performClick()
        compose.onNodeWithContentDescription("Добавить VPS").performClick()
        compose.waitUntil(5000) { compose.onAllNodesWithText("Новый VPS").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("203.0.113.10 или vpn.example.com").onLast().assertIsDisplayed()
        compose.onNodeWithText("Потоки").assertDoesNotExist()
        compose.onNodeWithText("Сохранить").assertIsDisplayed()
        screenshot("ui-vps-editor-review3.png")
        compose.onNodeWithText("Отмена").performClick()
    }

    @Test fun twoVpsProfilesCanBeAddedAndSelectedWithoutMixingTheirConnectionData() {
        compose.onNodeWithContentDescription("Настройки").performClick()
        compose.onNodeWithText("Участник").performClick()
        compose.onNodeWithText("Владелец").performClick()
        compose.onNodeWithContentDescription("Назад").performClick()
        compose.onNodeWithText("Деплой").performClick()
        val deploy = SettingsStore(compose.activity, deployScope = true)
        fun add(name: String, ip: String, ssh: String, peer: String, web: String): Int {
            compose.onNodeWithContentDescription("Добавить VPS").performClick()
            compose.waitUntil(5000) { compose.onAllNodesWithText("Новый VPS").fetchSemanticsNodes().isNotEmpty() }
            fun edit(label: String, value: String) { compose.onAllNodesWithContentDescription(label).onLast().performTextReplacement(value) }
            edit("Название сервера", name)
            edit("IP сервера", ip)
            edit("Порт SSH", ssh)
            edit("Логин SSH", "test-user")
            edit("Пароль SSH", "fixture-password")
            edit("Порт PEER", peer)
            edit("Порт WEB", web)
            compose.onNodeWithText("Сохранить").performClick()
            compose.waitUntil(5000) { compose.onAllNodesWithText("Новый VPS").fetchSemanticsNodes().isEmpty() }
            compose.waitUntil(5000) { runBlocking { deploy.deploySettingsSnapshot.first { it.isLoaded }.ip == ip } }
            return runBlocking { deploy.activeProfile.first() }
        }
        val suffix = System.nanoTime().toString().takeLast(6)
        val nameA = "VPS A $suffix"
        val nameB = "VPS B $suffix"
        val a = add(nameA, "203.0.113.81", "5052", "48100", "48102")
        val b = add(nameB, "203.0.113.82", "5053", "48200", "48202")
        check(a != b)
        compose.onNodeWithText(nameB).performClick()
        compose.onNodeWithText(nameA).performScrollTo().performClick()
        compose.waitUntil(5000) { runBlocking { deploy.activeProfile.first() == a } }
        compose.onNodeWithContentDescription("IP сервера").assertTextEquals("203.0.113.81")
        compose.onNodeWithContentDescription("Порт SSH").assertTextEquals("5052")
        runBlocking {
            val savedA = SettingsStore(compose.activity, readOnlyProfile = a).deploySettingsSnapshot.first { it.isLoaded }
            val savedB = SettingsStore(compose.activity, readOnlyProfile = b).deploySettingsSnapshot.first { it.isLoaded }
            check(savedA.serverPeerPort == 48100 && savedA.serverWebPort == 48102)
            check(savedB.ip == "203.0.113.82" && savedB.sshPort == "5053")
            check(savedB.serverPeerPort == 48200 && savedB.serverWebPort == 48202)
            check(store.activeProfile.first() == 0)
        }
    }
}
