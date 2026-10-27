package com.csqtt.client

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Client-side CPU wake-lock policy, adapted from Afteroid/csqttr PR #12. */
object BatterySaver {
    const val MODE_AUTO = "auto"
    const val MODE_LEVEL = "level"
    const val MODE_OFF = "off"
    const val DEFAULT_MODE = MODE_AUTO
    const val DEFAULT_PERCENT = 30
    val MODES = listOf(MODE_AUTO to "Авто", MODE_LEVEL to "Свой уровень", MODE_OFF to "Выкл")
    val PERCENT_CHOICES = listOf(10, 15, 20, 25, 30, 40, 50)
    const val TITLE = "Экономия батареи"
    const val LEVEL_TITLE = "Включать при заряде"
    const val DESCRIPTION = "Режимы\nАвто — вместе с энергосбережением телефона.\nСвой уровень — при выбранном заряде и ниже, когда телефон не заряжается.\nВыкл — процессор остаётся активным для туннеля.\n\nЧто изменится\nПри экономии приложение отпускает блокировку процессора. С выключенным экраном туннель может засыпать: сообщения и звонки через VPN могут приходить с задержкой, загрузки — останавливаться. Wi-Fi-блокировка сохраняется. Установка на сервере продолжает держать процессор активным."
    const val BADGE = "Экономия батареи: туннель может засыпать при выключенном экране"

    private val inForce = MutableStateFlow(false)
    val active = inForce.asStateFlow()
    internal fun setActive(value: Boolean) { inForce.value = value }

    fun shouldSave(mode: String, percent: Int, level: Int, charging: Boolean, systemSaver: Boolean): Boolean = when (mode) {
        MODE_AUTO -> systemSaver
        MODE_LEVEL -> !charging && level in 0..percent
        else -> false
    }
}
