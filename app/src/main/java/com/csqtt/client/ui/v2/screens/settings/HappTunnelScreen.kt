package com.csqtt.client.ui.v2.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.csqtt.client.*
import com.csqtt.client.ui.v2.components.*
import com.csqtt.client.ui.v2.design.V2Theme
import com.csqtt.client.ui.v2.dialogs.V2HashesDialog
import com.csqtt.client.ui.v2.dialogs.V2TurnSubnetsDialog
import com.csqtt.client.ui.v2.screens.connection.V2ProfilePickerMenu
import com.csqtt.client.ui.v2.screens.connection.V2EditProfileModal
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.Dispatchers
import kotlin.math.roundToInt

private val draftWrites = CoroutineScope(SupervisorJob() + Dispatchers.IO)

@Composable
fun HappTunnelScreen(settingsStore: SettingsStore, version: String, onSettings: () -> Unit, onVkAuthRequested: () -> Unit) {
    val c = V2Theme.colors
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val profile by settingsStore.activeProfile.collectAsStateWithLifecycle(0)
    val editingProfile = profile
    val names by settingsStore.profileNames.collectAsStateWithLifecycle(emptyMap())
    val occupied by settingsStore.occupiedProfiles.collectAsStateWithLifecycle(emptySet())
    val subscriptions by settingsStore.profileSubscriptions.collectAsStateWithLifecycle(emptyMap())
    val running by TunnelManager.running.collectAsStateWithLifecycle()
    val starting by TunnelManager.starting.collectAsStateWithLifecycle()
    val stopping by TunnelManager.stopping.collectAsStateWithLifecycle()
    val busy = running || starting || stopping
    val hashes by settingsStore.vkHashes.collectAsStateWithLifecycle("")
    val hashMode by settingsStore.vkHashMode.collectAsStateWithLifecycle("manual")
    val authMode by settingsStore.vkAuthMode.collectAsStateWithLifecycle("vkcalls")
    val token by settingsStore.vkAccessToken.collectAsStateWithLifecycle("")
    val srtp by settingsStore.srtpMode.collectAsStateWithLifecycle("auto")
    val transport by settingsStore.turnTransport.collectAsStateWithLifecycle("udp")
    val subnet by settingsStore.selectedTurnSubnet.collectAsStateWithLifecycle("")
    val ipLimit by settingsStore.turnSubnetIpLimit.collectAsStateWithLifecycle("ALL")
    val results by settingsStore.vkHashCheckResults.collectAsStateWithLifecycle("{}")
    val extraWorkers by settingsStore.extraWorkers.collectAsStateWithLifecycle(false)
    val workerMaximum = WorkerCountPolicy.defaultMaximum(extraWorkers && authMode != "auto_js")
    var loaded by remember(profile) { mutableStateOf(false) }
    var host by remember(profile) { mutableStateOf("") }
    var port by remember(profile) { mutableStateOf("46000") }
    var password by remember(profile) { mutableStateOf("") }
    var workers by remember(profile) { mutableIntStateOf(10) }
    var revision by remember(profile) { mutableLongStateOf(0L) }
    var picker by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Int?>(null) }
    var showHashes by remember { mutableStateOf(false) }
    var showSubnets by remember { mutableStateOf(false) }
    val effectiveWorkers = WorkerCountPolicy.normalize(workers, workerMaximum)
    val draftTracker = remember(profile) { mutableStateOf<ProfileConnectionSnapshot?>(null) }
    SideEffect {
        draftTracker.value = if (loaded && !busy) ProfileConnectionSnapshot(revision = revision, peer = host, serverPeerPort = port.toIntOrNull() ?: 0, connectionPassword = password, workersPerHash = effectiveWorkers) else null
    }
    DisposableEffect(profile) {
        val slot = profile
        onDispose {
            val draft = draftTracker.value
            if (draft != null && draft.serverPeerPort in 1..65535) draftWrites.launch {
                settingsStore.saveProfileConnectionSettings(slot, draft.peer, draft.serverPeerPort, draft.connectionPassword, draft.workersPerHash, expectedRevision = draft.revision)
            }
        }
    }
    LaunchedEffect(profile) {
        val saved = settingsStore.readProfileConnectionSettings(profile)
        host = saved.peer; port = saved.serverPeerPort.toString(); password = saved.connectionPassword
        workers = saved.workersPerHash; revision = saved.revision; loaded = true
    }
    LaunchedEffect(profile, loaded, revision, host, port, password, effectiveWorkers) {
        if (!loaded || busy) return@LaunchedEffect
        delay(350)
        val validPort = port.toIntOrNull()?.takeIf { it in 1..65535 } ?: return@LaunchedEffect
        settingsStore.saveProfileConnectionSettings(profile, host, validPort, password, effectiveWorkers, expectedRevision = revision)
    }
    fun changeHash(label: String) { scope.launch { settingsStore.saveVkHashMode(when(label) { "Авто API" -> "auto_api"; "Авто ВК" -> "auto_js"; else -> "manual" }, forProfile = editingProfile) } }
    val parts = hashes.split(Regex("[,\\s]+" )).filter(String::isNotBlank)
    val green = c.success
    val orange = c.warning
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        V2TopBar(version = version, title = "Туннель", onSettings = onSettings)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            V2Card(onClick = { picker = true }, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 9.dp), borderColor = c.outline) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(names[profile] ?: defaultProfileName(profile), fontSize = 14.sp); Text("$host:$port", color = c.textSecondary, fontSize = 11.sp) }
                    TextButton(onClick = { editing = profile }) { Text("Изменить", fontSize = 12.sp) }
                }
            }
            V2Card(contentPadding = PaddingValues(12.dp), borderColor = c.outline) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HappField("Адрес сервера", host, { host = it }, Modifier.weight(1.65f), enabled = loaded && !busy)
                    HappField("Порт PEER", port, { port = it.filter(Char::isDigit).take(5) }, Modifier.weight(1f), number = true, enabled = loaded && !busy)
                }
                Spacer(Modifier.height(7.dp))
                HappField("Пароль туннеля", password, { password = it }, password = true, enabled = loaded && !busy)
                if (hashMode == "manual") {
                    OutlinedButton(onClick = { showHashes = true }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(44.dp), enabled = !busy) { Text("Настроить хеши") }
                } else {
                    Button(onClick = onVkAuthRequested, modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(44.dp), enabled = !busy) { Text(if (token.isNotBlank()) "Аккаунт VK подключён" else "Войти в VK") }
                }
                HappChoice("Режим хешей", when(hashMode) { "auto_api" -> "Авто API"; "auto_js" -> "Авто ВК"; else -> "Ручные хеши" }, listOf("Ручные хеши", "Авто API", "Авто ВК"), ::changeHash,
                    tone = if(hashMode == "auto_js") orange else green, enabled = !busy,
                    help = "Ручные хеши — шесть хешей звонков без входа в VK. Авто API — получение через API VK. Авто ВК — получение через авторизованный аккаунт VK.")
                HappChoice("Режим кредов", when(authMode) { "auto_js" -> "Авто ВК"; "legacy" -> "Капча"; else -> "Авто" }, listOf("Авто", "Капча", "Авто ВК"), { label -> scope.launch { settingsStore.saveVkAuthMode(when(label) { "Авто ВК" -> "auto_js"; "Капча" -> "legacy"; else -> "vkcalls" }, forProfile = editingProfile) } },
                    tone = if(authMode == "vkcalls") green else orange, enabled = !busy, help = "Авто — гостевые TURN-креды VK Calls. Капча — браузерный способ с возможной капчей. Авто ВК — креды авторизованного аккаунта.")
                HappChoice("Режим SRTP", when(srtp) { "dtls-srtp" -> "DTLS-SRTP"; "wrap-srtp" -> "WRAP-SRTP"; else -> "Авто" }, listOf("Авто", "DTLS-SRTP", "WRAP-SRTP"), { label -> scope.launch { settingsStore.saveSrtpMode(when(label) { "DTLS-SRTP" -> "dtls-srtp"; "WRAP-SRTP" -> "wrap-srtp"; else -> "auto" }, forProfile = editingProfile) } },
                    tone = if(srtp == "wrap-srtp") orange else green, enabled = !busy, help = "Авто — DTLS-SRTP с переходом к WRAP-SRTP. DTLS-SRTP — настоящее рукопожатие. WRAP-SRTP — SRTP-обёртка без DTLS-рукопожатия.")
                HappChoice("Транспорт", if(transport == "udp") "UDP" else "TCP", listOf("UDP", "TCP"), { label -> scope.launch { settingsStore.saveTurnTransport(if(label == "UDP") "udp" else CsqttConstants.Tunnel.TURN_TRANSPORT_TCP_TLS, forProfile = editingProfile) } },
                    tone = if(transport == "udp") green else orange, enabled = !busy, help = "UDP — основной транспорт TURN. TCP/TLS — альтернатива, если UDP недоступен.")
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Потоки", Modifier.weight(1f), fontSize = 13.sp)
                    HappHelp("Потоки", "Число параллельных TURN-потоков. Настройка относится к текущему профилю.")
                    Surface(color = c.accentSoft, shape = RoundedCornerShape(10.dp)) { Text(effectiveWorkers.toString(), color = c.accent, fontSize = 15.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) }
                }
                V2PowerSlider(effectiveWorkers.toFloat(), maximum = workerMaximum.toFloat(), enabled = !busy,
                    onValueChange = { workers = WorkerCountPolicy.normalize(it.roundToInt(), workerMaximum) }, showHeader = false)
                HappActionRow("TURN подсети", subnet.ifBlank { "Автоматически" }, { showSubnets = true })
            }
            Spacer(Modifier.height(4.dp))
        }
    }
    if (picker) Dialog(onDismissRequest = { picker = false }) { V2ProfilePickerMenu(settingsStore, profile, names, occupied, subscriptions, busy, { picker = false }, { editing = it }) }
    editing?.let { slot -> Dialog(onDismissRequest = { editing = null }) { V2EditProfileModal(settingsStore, slot, names[slot] ?: defaultProfileName(slot), isNew = false, onSaved = { loaded = false; scope.launch { val saved = settingsStore.readProfileConnectionSettings(profile); host = saved.peer; port = saved.serverPeerPort.toString(); password = saved.connectionPassword; workers = saved.workersPerHash; revision = saved.revision; loaded = true } }, onDismiss = { editing = null }) } }
    if (showHashes) V2HashesDialog(parts.getOrElse(0){""}, parts.getOrElse(1){""}, parts.getOrElse(2){""}, parts.getOrElse(3){""}, parts.getOrElse(4){""}, parts.getOrElse(5){""},
        VkHashValidationCodec.decode(results), { scope.launch { settingsStore.saveVkHashCheckResults(VkHashValidationCodec.encode(it)) } },
        { list -> VkHashValidator.check(context, list, settingsStore.selectedFingerprint.first(), settingsStore.activeClientIds.first()) },
        onSave = { a,b,d,e,f,g -> scope.launch { settingsStore.saveVkHashesOnly(listOf(a,b,d,e,f,g).filter(String::isNotBlank).distinct().joinToString(",")) } }, onDismiss = { showHashes = false })
    if (showSubnets) V2TurnSubnetsDialog(subnet, ipLimit, { scope.launch { settingsStore.saveSelectedTurnSubnet(it, forProfile = editingProfile) } }, { scope.launch { settingsStore.saveTurnSubnetIpLimit(it, forProfile = editingProfile) } }, { showSubnets = false })
}
