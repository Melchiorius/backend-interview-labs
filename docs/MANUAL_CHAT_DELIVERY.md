# Передача поручений в существующие чаты без Selenium

**Назначение:** локальный пилот для Windows с Firefox, установленным браузером по умолчанию. Используется вместе с [GitHub-диспетчером](../tools/role_dispatcher.py) и [протоколом handoff](ROLE_HANDOFF_PROTOCOL.md). Не требует платного OpenAI API, Selenium, Marionette или парсинга HTML.

Python **3.10+**, только стандартная библиотека. На Windows используйте `py`, если `python` указывает на заглушку WindowsApps. Команды ниже выполняются **из корня клонированного репозитория** после объединения PR, содержащего этот скрипт; до merge используйте соответствующую ветку или скачанный архив.

## Тест без GitHub и без автоматической отправки

1. Откройте существующий чат роли 06 и скопируйте его URL из адресной строки Firefox. Сохраните сопоставление (замените URL на настоящий):

   ```powershell
   py tools/manual_chat_delivery.py configure --role 06 --url "https://chatgpt.com/c/ВАШ-ID-ЧАТА"
   ```

   Для чатов внутри проекта допустим также URL вида `https://chatgpt.com/g/g-p-.../c/...`. Можно настроить все роли 00–06 аналогичным образом. URL сохраняются в `.dispatcher/chat_urls.json` — **локально**, не в GitHub.

2. Создайте безвредное тестовое поручение:

   ```powershell
   New-Item -ItemType Directory -Force .dispatcher\inbox\06 | Out-Null
   "Это тест. Ответь одним словом: ПОЛУЧЕНО." | Set-Content -Encoding UTF8 .dispatcher\inbox\06\demo-event-0001.md
   ```

3. Проверка маршрута **без** открытия браузера и копирования:

   ```powershell
   py tools/manual_chat_delivery.py status
   py tools/manual_chat_delivery.py prepare --role 06 --dry-run
   ```

4. Подготовьте текст к ручной отправке:

   ```powershell
   py tools/manual_chat_delivery.py prepare --role 06
   ```

   Скрипт скопирует текст в буфер обмена и откроет URL чата обычным браузером по умолчанию. Браузер **может открыть новую вкладку**, даже если чат уже открыт. Проверьте адрес, щёлкните в поле ввода и нажмите **Ctrl+V**, затем **Enter**. Python не взаимодействует с HTML страницы и **не нажимает Enter сам**.

5. **Только после фактической отправки сообщения** подтвердите доставку:

   ```powershell
   py tools/manual_chat_delivery.py confirm --role 06 --event demo-event-0001
   py tools/manual_chat_delivery.py status
   ```

   Если вставка или отправка не удалась, **не подтверждайте событие**. Оно останется в очереди и может быть подготовлено повторно. Диспетчер не способен автоматически удостовериться, что чат принял сообщение.

## Реальная работа через GitHub

Диспетчер читает комментарии Issues, проверяет внешние условия и создаёт сообщения `.dispatcher/inbox/<роль>/<event_id>.md`. Для приватного репозитория GitHub-токен берётся из переменной окружения и никогда не записывается в Git:

```powershell
$env:GITHUB_TOKEN = "<GitHub token>"
py tools/role_dispatcher.py --issue 33
py tools/manual_chat_delivery.py status
py tools/manual_chat_delivery.py prepare
```

`--issue 33` — пример номера Issue; обработаются только реальные структурированные поручения. При желании запустите `py tools/role_dispatcher.py --interval 120` в **другом терминале**; Python будет отслеживать новые поручения, но для отправки в ChatGPT по-прежнему нужны Ctrl+V, Enter и `confirm`.

Проверки без сети и без обращения к браузеру:

```powershell
py -m py_compile tools\manual_chat_delivery.py tools\role_dispatcher.py
py -m unittest discover -s tools/tests -v
```

## Границы и безопасность

- Не читает страницы ChatGPT, не определяет готовность ответа по DOM, не нажимает кнопки браузера. GitHub остаётся каналом результатов и новых handoff.
- Не является полной автоматизацией чатов и не проверяет содержательную правильность сообщений. Состояние отправки фиксируется только после команды человека `confirm`.
- Один event ID нельзя незаметно заменить другим текстом: после подготовки изменение содержимого отвергается, требуется новое событие. Подтверждения хранятся в `.dispatcher/manual_delivery.sqlite3`.
- Windows-буфер обмена перезаписывается текстом поручения. Не кладите туда токены и секреты; не отправляйте секреты в чат.
- Код проверен автономными тестами; **реальная связка с Firefox и Windows Clipboard должна быть проверена на компьютере пользователя**.