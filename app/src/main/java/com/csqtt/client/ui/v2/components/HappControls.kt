package com.csqtt.client.ui.v2.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.csqtt.client.ui.v2.design.V2Theme

@Composable
internal fun HappField(label: String, value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier,
    password: Boolean = false, number: Boolean = false, enabled: Boolean = true) {
    val c = V2Theme.colors
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = c.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        BasicTextField(value, onChange, modifier = Modifier.fillMaxWidth().height(44.dp),
            singleLine = true, enabled = enabled,
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, color = c.textPrimary),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = when { password -> KeyboardType.Password; number -> KeyboardType.Number; else -> KeyboardType.Text }),
            decorationBox = { inner ->
                Surface(shape = RoundedCornerShape(14.dp), color = c.field, border = BorderStroke(1.dp, c.outline)) {
                    Box(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.CenterStart) { inner() }
                }
            })
    }
}

@Composable
internal fun HappHelp(title: String, text: String) {
    var open by remember { mutableStateOf(false) }
    IconButton(onClick = { open = true }, modifier = Modifier.size(36.dp)) {
        Icon(Icons.AutoMirrored.Outlined.HelpOutline, "Подсказка: $title", tint = V2Theme.colors.textSecondary, modifier = Modifier.size(17.dp))
    }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text(title) }, text = { Text(text) },
        confirmButton = { TextButton(onClick = { open = false }) { Text("Понятно") } })
}

@Composable
internal fun HappChoice(label: String, value: String, options: List<String>, onSelect: (String) -> Unit,
    modifier: Modifier = Modifier, tone: Color? = null, help: String? = null, enabled: Boolean = true) {
    val c = V2Theme.colors
    var expanded by remember { mutableStateOf(false) }
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), color = c.textPrimary, fontSize = 13.sp)
        if (help != null) HappHelp(label, help)
        Box(Modifier.widthIn(min = 128.dp, max = 158.dp)) {
            Surface(onClick = { if (enabled) expanded = true }, enabled = enabled, shape = RoundedCornerShape(14.dp),
                color = c.field, border = BorderStroke(1.dp, c.outline)) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(value, modifier = Modifier.weight(1f), fontSize = 13.sp, maxLines = 1, color = c.textPrimary)
                        Icon(Icons.Default.KeyboardArrowDown, null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
                    }
                    if (tone != null) Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(3.dp).background(tone, RoundedCornerShape(2.dp)))
                }
            }
            DropdownMenu(expanded, { expanded = false }, containerColor = c.surface) {
                options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { expanded = false; onSelect(option) }) }
            }
        }
    }
}

@Composable
internal fun HappActionRow(title: String, detail: String = "", onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = V2Theme.colors
    Row(modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(onClick = onClick), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), fontSize = 13.sp, color = c.textPrimary)
        Text(detail, color = c.textSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 150.dp))
        Text(" ›", color = c.textSecondary, fontSize = 22.sp)
    }
}
