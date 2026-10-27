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
}
