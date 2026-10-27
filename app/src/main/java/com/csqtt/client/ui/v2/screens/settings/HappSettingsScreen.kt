package com.csqtt.client.ui.v2.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.csqtt.client.SettingsStore
import com.csqtt.client.ui.v2.components.*
import com.csqtt.client.ui.v2.dialogs.V2ImportExportDialog
import kotlinx.coroutines.launch

@Composable
fun HappSettingsScreen(settingsStore: SettingsStore, version: String, onBack: () -> Unit, onAbout: () -> Unit) {
    val scope = rememberCoroutineScope()
    val owner by settingsStore.uiOwnerMode.collectAsStateWithLifecycle(false)
    val extra by settingsStore.extraWorkers.collectAsStateWithLifecycle(false)
    val wifi by settingsStore.autoPauseOnWifi.collectAsStateWithLifecycle(false)
    val logs by settingsStore.loggingEnabled.collectAsStateWithLifecycle(true)
    val theme by settingsStore.themeMode.collectAsStateWithLifecycle("system")
    val fingerprint by settingsStore.selectedFingerprint.collectAsStateWithLifecycle("chrome")
    var transfer by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        V2TopBar(title = "Настройки", onBack = onBack)
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            V2Card {
                Text("CSQTTR", style = MaterialTheme.typography.titleLarge)
                Text(version, style = MaterialTheme.typography.bodySmall)
                HappChoice("Режим приложения", if(owner) "Владелец" else "Участник", listOf("Участник", "Владелец"), { scope.launch { settingsStore.saveUiOwnerMode(it == "Владелец") } },
                    help = "Участник подключается по профилю. Владелец дополнительно видит вкладку «Деплой». По умолчанию выбран участник.")
            }
            V2Card(contentPadding = PaddingValues(12.dp)) {
                V2SwitchSetting("Экстра потоки", extra, { scope.launch { settingsStore.saveExtraWorkers(it) } })
                V2SwitchSetting("Автопауза Wi-Fi", wifi, { scope.launch { settingsStore.saveAutoPauseOnWifi(it) } })
                V2SwitchSetting("Ведение логов", logs, { scope.launch { settingsStore.saveLoggingEnabled(it) } })
            }
            V2Card(contentPadding = PaddingValues(12.dp)) {
                HappChoice("Оформление", when(theme) { "light" -> "Светлое"; "dark" -> "Тёмное"; else -> "Системное" }, listOf("Тёмное", "Светлое", "Системное"), { scope.launch { settingsStore.saveThemeMode(when(it) { "Тёмное" -> "dark"; "Светлое" -> "light"; else -> "system" }) } })
                HappChoice("Отпечаток TLS", fingerprint, listOf("chrome", "firefox", "safari"), { scope.launch { settingsStore.saveFingerprint(it) } })
                HappActionRow("Импорт / экспорт", onClick = { transfer = true })
                HappActionRow("О приложении", onClick = onAbout)
            }
        }
    }
    if (transfer) V2ImportExportDialog(settingsStore, { transfer = false })
}
