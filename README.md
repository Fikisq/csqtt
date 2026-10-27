<div align="center">

# CSQTT-Rebirth

<br>

<img src="https://img.shields.io/badge/Android-SDK_28--37-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android SDK" />
<img src="https://img.shields.io/badge/Rust-2024-000000?style=for-the-badge&logo=rust&logoColor=white" alt="Rust Core" />
<img src="https://img.shields.io/badge/Kotlin-Compose-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
<a href="https://github.com/amurcanov/csqtt/stargazers">
  <img src="https://img.shields.io/github/stars/amurcanov/csqtt?style=for-the-badge&logo=github&color=ffca28&labelColor=24292e" alt="Stars" />
</a>

[CSQTT Telegram Community](https://t.me/wdttcommunity)

</div>

<br>

**CSQTT** — Android-приложение и высокопроизводительный сервер для создания защищённого туннеля поверх TURN/RTP. Клиент поднимает локальный интерфейс на устройстве, маскируя транспорт под зашифрованный медиатрафик звонка.

**Кросс., проекты с поддержкой CSQTT**: 
- Windows & Linux порт - [luminescq/focsq](https://github.com/luminescq/focsq)
- ios совместимый клиент - [anton48/vk-turn-proxy-ios](https://github.com/anton48/vk-turn-proxy-ios)

**Прекрасный аналог csqtt / wdtt**: [hackdiaz-dev/turn-proxy-android](https://github.com/hackdiaz-dev/turn-proxy-android)

> [!WARNING]
> **Некоммерческий статус и запрет коммерческого использования**
> Проект является строго некоммерческим исследовательским инструментом и не преследует извлечения выгоды.
> 
> Я (**amurcanov**) прямо запрещаю любое использование исходного кода данного репозитория ([github.com/amurcanov/csqtt](https://github.com/amurcanov/csqtt)) в коммерческих целях в соответствии с условиями лицензии **PolyForm Noncommercial License 1.0.0**. Любая продажа, перепродажа, интеграция в платные сервисы или извлечение прибыли на базе данного кода запрещены.

> [!NOTE]
> Подробная техническая информация и документация по архитектуре **CSQTT** будут опубликованы позже в отдельном окне заметок.

---

## SSH-порт при деплое

В параметрах сервера укажите фактический порт SSH на VPS (по умолчанию — 22).
В интерфейсе V2 этот порт используется независимо от переключателя
«Ручные порты (Peer, Web)»: переключатель управляет только портами сервера
CSQTT и веб-панели. Номер SSH-порта сохраняется при переключении этого режима.

Если ранее введённый SSH-порт сбросился на 22, введите его заново и сохраните
параметры сервера. Отказ соединения на SSH-порту возникает до проверки пароля;
работающее подключение VLESS не подтверждает доступность SSH.

На этапе установления SSH-соединения используются тайм-ауты 15 секунд.
При сетевом тайм-ауте клиент закрывает сокет и один раз повторяет подключение;
этот повтор выполняется до аутентификации и запуска любых команд на VPS.
Ошибки ключа сервера и аутентификации автоматически не повторяются.

Для отдельной тестовой установки, сохраняющей установленную debug-версию,
соберите `assembleDebug -Pcsqtt.abis=arm64-v8a -Pcsqtt.debugApplicationIdSuffix=.sshfix -Pcsqtt.debugAppLabel="CSQTTR SSH Fix"`.
Пакет будет называться `csqtt.quic.amurcanov.sshfix`; без этого параметра
debug-сборка сохраняет прежний суффикс `.forktest`.

## Лицензия

Этот проект распространяется под лицензией **PolyForm Noncommercial License 1.0.0 (PolyForm-Noncommercial-1.0.0)**.
