# LastLineControl

<p align="center">
  <a href="https://papermc.io"><img src="https://img.shields.io/badge/Платформа-PaperMC_1.21+-1976D2?style=for-the-badge&logo=minecraft&logoColor=white" alt="PaperMC 1.21+"></a>
  <a href="https://oracle.com/java"><img src="https://img.shields.io/badge/Среда-Java_21-ED8B00?style=for-the-badge&logo=java&logoColor=white" alt="Java 21"></a>
  <a href="https://maven.apache.org"><img src="https://img.shields.io/badge/Сборка-Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white" alt="Maven"></a>
  <a href="https://modrinth.com"><img src="https://img.shields.io/badge/Обновления-Modrinth_API-00AF5C?style=for-the-badge&logo=modrinth&logoColor=white" alt="Modrinth"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/Лицензия-MIT-gray?style=for-the-badge" alt="Лицензия"></a>
</p>

Высокопроизводительный серверный плагин для PaperMC (Minecraft 1.21+), предоставляющий профессиональное управление режимами полета (`/fly`) и неуязвимости (`/god`) с надежным сохранением состояния игроков между перезапусками сервера и тонкой системой ограничений действий.

Идеально подходит для серверов выживания, анархии и гриф-проектов: предотвращает абьюзы механик полета и бессмертия при фарме ресурсов и в сражениях благодаря гибко настраиваемым bypass-разрешениям.

## Основные возможности

* **Персистентность состояний**: Режимы `/fly` и `/god` надежно сохраняются при выходе игрока с сервера и полных перезапусках инстанса.
* **Глубокая матрица ограничений**: Возможность выборочно блокировать в режимах полета и бога ломание/установку блоков, PVP-урон, поднятие дропа, взаимодействие с контейнерами, сундуками Края, кристаллами Энда и транспортом.
* **Гибкая система обхода (Bypass Permissions)**: Выдача привилегированным группам прав на игнорирование конкретных ограничений.
* **Административное управление**: Управление режимами полета и бога для любых игроков на сервере прямо из консоли или чата.
* **Автопроверка обновлений**: Встроенный чекер новых версий через официальное API Modrinth с автоматическим уведомлением операторов сервера при входе.

## Команды

| Команда | Описание | Право доступа |
|---|---|---|
| `/fly` | Переключить режим полета для себя | `lastline.fly` |
| `/fly <никнейм>` | Переключить полет для указанного игрока | `lastline.admin` |
| `/god` | Переключить режим неуязвимости для себя | `lastline.god` |
| `/god <никнейм>` | Переключить бессмертие для другого игрока | `lastline.admin` |
| `/llc reload` | Горячая перезагрузка конфигурационных файлов | `lastline.admin` |

## Права доступа (Permissions)

| Право | Назначение | Значение по умолчанию |
|---|---|---|
| `lastline.admin` | Полный административный доступ к командам плагина | `op` |
| `lastline.fly` | Использование команды `/fly` для себя | `false` |
| `lastline.god` | Использование команды `/god` для себя | `false` |
| `lastline.bypass.<действие>` | Право на обход конкретного ограничения | `false` |

### Доступные действия для bypass:
`pickup`, `block-break`, `block-place`, `drop-item`, `pvp`, `hit-mob`, `chest`, `containers`, `interact-bow`, `interact-crystal`, `interact-flint`, `interact-enderchest`, `interact-shulker`, `interact-entities`, `projectiles`, `vehicles`, `spawn-eggs`.

## Быстрый старт

### Установка плагина
1. Поместите скомпилированный файл `lastlinecontrol-x.x.x.jar` в папку `plugins/` вашего сервера.
2. Перезапустите сервер или загрузите плагин через менеджер плагинов.
3. Отредактируйте сгенерированный файл `plugins/LastLineControl/config.yml` под правила вашего проекта.
4. Выполните команду `/llc reload` для применения изменений без перезагрузки сервера.

### Сборка из исходников

```bash
# Клонирование репозитория
git clone https://github.com/KreziBro/lastlinecontrol.git
cd lastlinecontrol

# Компиляция jar через Maven
mvn clean package
```

Готовый артефакт появится в каталоге `target/`.

## Стек технологий

* **Платформа**: [PaperMC](https://papermc.io/) API (Minecraft 1.21+)
* **Язык**: Java 21
* **Система сборки**: Apache Maven

## Сообщество и поддержка

* Официальный Telegram канал разработчика: https://t.me/KreziBro_dev
* Поддержать развитие проекта на платформе Ko-fi: https://ko-fi.com/krejzibro

## Лицензия

Распространяется под лицензией MIT. Подробности в файле [LICENSE](LICENSE).
