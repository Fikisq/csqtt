package com.csqtt.client.ui.v2.screens.connection

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.csqtt.client.SettingsStore
import com.csqtt.client.defaultProfileName
import com.csqtt.client.ui.v2.design.V2Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/** ICMP round trip, not a made-up VPN latency. A VPS may block ICMP while its tunnel still works. */
internal suspend fun measureServerPing(host: String): Int? = withContext(Dispatchers.IO) {
    if (host.isBlank() || host.startsWith('-') || host.any { it.isWhitespace() }) return@withContext null
    runCatching {
        val process = ProcessBuilder("/system/bin/ping", "-c", "1", "-W", "2", host).redirectErrorStream(true).start()
        try {
            if (!process.waitFor(3, TimeUnit.SECONDS)) return@runCatching null
            val output = process.inputStream.bufferedReader().use { it.readText().take(4096) }
            Regex("time[=<]([0-9.]+)").find(output)?.groupValues?.get(1)?.toDoubleOrNull()?.toInt()?.coerceAtLeast(1)
        } finally { process.destroy() }
    }.getOrNull()
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun HappServerList(store: SettingsStore, active: Int, busy: Boolean, onEdit: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = V2Theme.colors
    val scope = rememberCoroutineScope()
    val occupied by store.occupiedProfiles.collectAsStateWithLifecycle(emptySet())
    val names by store.profileNames.collectAsStateWithLifecycle(emptyMap())
    val servers by store.deployServers.collectAsStateWithLifecycle(emptyList())
    val hosts = remember { mutableStateMapOf<Int,String>() }
    val latency = remember { mutableStateMapOf<Int,Int?>() }
    val checking = remember { mutableStateMapOf<Int,Boolean>() }
    LaunchedEffect(occupied, names, servers) {
        occupied.forEach { id -> hosts[id] = store.readProfileConnectionSettings(id).endpoint()?.host.orEmpty() }
    }
    fun ping(id: Int) {
        if(checking[id] == true) return
        scope.launch { checking[id] = true; try { latency[id] = measureServerPing(hosts[id].orEmpty()) } finally { checking[id] = false } }
    }
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Серверы", Modifier.weight(1f), color = c.textPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            TextButton(onClick = { occupied.forEach(::ping) }, enabled = checking.values.none { it }) { Text(if(checking.values.any { it }) "Проверка…" else "Пинг") }
        }
        Column(Modifier.fillMaxWidth().heightIn(max = 280.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            if(occupied.isEmpty()) Text("Нет профилей · добавьте через +", fontSize = 13.sp, color = c.textSecondary, modifier = Modifier.padding(vertical = 20.dp))
            occupied.sorted().forEach { id ->
                val core = servers.firstOrNull { it.profile == id }?.installedVersion?.takeIf(String::isNotBlank)?.let { "CSQTT $it" } ?: "CSQTT"
                Surface(color = if(active == id) c.accentSoft else c.surface, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp,c.outline),
                    modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { if(!busy) scope.launch { store.saveActiveProfile(id) } }, onLongClick = { onEdit(id) })) {
                    Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(names[id] ?: defaultProfileName(id), color = c.textPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("Ядро: $core", color = c.textSecondary, fontSize = 11.sp)
                        }
                        TextButton(onClick = { ping(id) }) { Text(if(checking[id] == true) "…" else if(latency.containsKey(id)) latency[id]?.let { "$it мс" } ?: "—" else "Пинг", color = if((latency[id] ?: 0) > 100) c.warning else c.success, fontSize = 12.sp) }
                    }
                }
            }
        }
    }
}
