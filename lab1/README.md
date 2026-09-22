# Lab 1 Secure REST API

Учебный REST API на Java 17, Spring Boot, Spring Security, Spring Data JPA и H2. Проект демонстрирует базовые меры защиты из OWASP Top 10 и запуск security-проверок в CI/CD.

## Быстрый запуск

```bash
./gradlew bootRun
```

Windows:

```powershell
.\gradlew.bat bootRun
```

Тестовый пользователь создается автоматически:

- login: `admin`
- password: `local-demo-password`

Для публичного репозитория реальные учетные данные и JWT secret не хранятся в коде. Для локального запуска их можно переопределить переменными окружения:

- `APP_SEED_USERNAME`
- `APP_SEED_PASSWORD`
- `APP_SEED_ROLE`
- `APP_JWT_SECRET`

## API

### POST `/auth/login`

Аутентификация пользователя и выдача JWT.

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"admin\",\"password\":\"local-demo-password\"}"
```

Пример ответа:

```json
{
  "token": "eyJraWQiOi...",
  "tokenType": "Bearer"
}
```

### GET `/api/data`

Возвращает список постов. Требует JWT в заголовке `Authorization`.

```bash
curl http://localhost:8080/api/data \
  -H "Authorization: Bearer <JWT>"
```

Без токена API возвращает `401 Unauthorized`.

### POST `/api/posts`

Создает новый пост от имени аутентифицированного пользователя.

```bash
curl -X POST http://localhost:8080/api/posts \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d "{\"title\":\"New post\",\"body\":\"Secure API example\"}"
```

## Реализованные меры защиты

**SQL Injection.** Работа с базой выполнена через Spring Data JPA repositories (`AppUserRepository`, `PostRepository`). SQL-запросы формируются ORM и параметризуются, конкатенация пользовательского ввода в SQL не используется.

**XSS.** Пользовательские строки экранируются перед возвратом из API через `HtmlUtils.htmlEscape(...)`. Это применяется к заголовку, тексту поста и имени автора в DTO ответа.

**Broken Authentication.** Пароли не хранятся в открытом виде: seed-пользователь сохраняется с bcrypt-хэшем через `BCryptPasswordEncoder(12)`. При успешном логине API выдает JWT. Защищенные эндпоинты проверяются `JwtAuthenticationFilter`, который принимает только токены из `Authorization: Bearer ...`.

**Ограничение ввода.** DTO используют Bean Validation: обязательные поля и лимиты длины для логина, пароля, заголовка и текста поста.

## CI/CD и security checks

Pipeline находится в `.github/workflows/ci.yml` и запускается при `push` и `pull_request`.

Этапы:

1. `./gradlew test` - интеграционные тесты API.
2. `./gradlew spotbugsMain` - SAST-проверка Java-кода.
3. `./gradlew dependencyCheckAnalyze` - SCA-проверка зависимостей через OWASP Dependency-Check.
4. Загрузка HTML/JSON отчетов как GitHub Actions artifact `security-reports`.

Скриншоты успешных отчетов SAST/SCA нужно добавить после первого запуска GitHub Actions в публичном репозитории:

![GitHub Actions successful run](docs/screenshots/actions-success.png)
![SpotBugs report](docs/screenshots/spotbugs-report.png)
![OWASP Dependency-Check report](docs/screenshots/dependency-check-report.png)

## Локальная проверка

```bash
./gradlew test
./gradlew spotbugsMain
./gradlew dependencyCheckAnalyze
```

## Ссылки для сдачи

- Репозиторий: `https://github.com/yaroslavesev/cyber-security-course`
- Последний успешный pipeline: `<добавьте ссылку на успешный запуск Actions/CI>`
