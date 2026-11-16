# mytracker

Личный каталог фильмов, сериалов и аниме. Проект постепенно разрабатывается на Kotlin Multiplatform и Compose Multiplatform.

## Текущий этап

- Настроен общий исходный код Kotlin Multiplatform и точка запуска для Windows/JVM.
- Добавлен начальный тёмный экран каталога.
- Определены базовые модели категорий, произведений, статусов, сезонов и впечатлений.
- Сохранение данных пока не подключено.

## Запуск в IntelliJ IDEA

Нужен JDK 25. В IntelliJ IDEA укажите его в **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JVM**, затем откройте проект как Gradle-проект и дождитесь синхронизации. Для запуска настольной версии выберите конфигурацию JVM с Compose Hot Reload.

## Запуск из PowerShell

Установите `JAVA_HOME` на папку JDK 25, затем из корня проекта выполните:

```powershell
./gradlew.bat run
```

Gradle Wrapper сам загрузит Gradle 9.8.2 при первом запуске. Wrapper включает `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar` и настройки версии Gradle.

## Структура

- `src/commonMain` — общий интерфейс и модели данных.
- `src/jvmMain` — точка запуска настольной версии.
- `gradle/wrapper` — настройки версии Gradle Wrapper.

