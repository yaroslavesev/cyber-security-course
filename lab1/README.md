# Lab 1 Secure REST API

Учебный REST API на Java 17, Spring Boot, Spring Security, Spring Data JPA и H2. Проект демонстрирует базовую защиту от распространенных веб-уязвимостей: SQL Injection, XSS и проблем аутентификации. В CI/CD запускаются тесты, SAST-проверка SpotBugs и SCA-проверка OWASP Dependency-Check.

## Быстрый запуск

Linux/macOS:

```bash
./gradlew bootRun
```

Windows:

```powershell
.\gradlew.bat bootRun
```

После запуска API доступен по адресу:

```text
http://localhost:8080
```

Тестовый пользователь создается автоматически при старте приложения:

```text
login: admin
password: local-demo-password
```

Параметры можно переопределить переменными окружения:

- `APP_SEED_USERNAME` - имя seed-пользователя.
- `APP_SEED_PASSWORD` - пароль seed-пользователя.
- `APP_SEED_ROLE` - роль seed-пользователя, по умолчанию `ROLE_USER`.
- `APP_JWT_SECRET` - секрет для подписи JWT. Для production его нужно хранить вне репозитория.

## API

### POST `/auth/login`

Аутентифицирует пользователя и возвращает JWT.

Request:

```http
POST /auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "local-demo-password"
}
```

cURL:

```bash
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"local-demo-password"}'
```

Response `200 OK`:

```json
{
  "token": "<JWT>",
  "tokenType": "Bearer"
}
```

При неверных учетных данных возвращается `401 Unauthorized`.

### GET `/api/data`

Возвращает список постов. Эндпоинт защищен JWT.

Request:

```http
GET /api/data
Authorization: Bearer <JWT>
```

cURL:

```bash
curl http://localhost:8080/api/data \
  -H "Authorization: Bearer <JWT>"
```

Response `200 OK`:

```json
[
  {
    "id": 1,
    "title": "Security checklist",
    "body": "Use JWT, bcrypt and parameterized queries.",
    "author": "admin"
  }
]
```

Без токена или с некорректным токеном возвращается `401 Unauthorized`.

### POST `/api/posts`

Создает новый пост от имени аутентифицированного пользователя. Эндпоинт защищен JWT.

Request:

```http
POST /api/posts
Authorization: Bearer <JWT>
Content-Type: application/json

{
  "title": "New post",
  "body": "Secure API example"
}
```

cURL:

```bash
curl -X POST http://localhost:8080/api/posts \
  -H "Authorization: Bearer <JWT>" \
  -H "Content-Type: application/json" \
  -d '{"title":"New post","body":"Secure API example"}'
```

Response `201 Created`:

```json
{
  "id": 3,
  "title": "New post",
  "body": "Secure API example",
  "author": "admin"
}
```

Тело запроса валидируется: `title` обязателен и ограничен 120 символами, `body` обязателен и ограничен 1000 символами.

## Реализованные меры защиты

### Защита от SQL Injection

Работа с базой данных выполняется через Spring Data JPA repositories: `AppUserRepository` и `PostRepository`. В проекте нет ручной сборки SQL-строк из пользовательского ввода. Методы вроде `findByUsername(...)` преобразуются Spring Data JPA в параметризованные запросы, поэтому пользовательские значения передаются как параметры, а не как часть SQL-кода.

Сущности дополнительно ограничивают структуру данных через JPA-аннотации:

- `AppUser.username` уникален и ограничен 64 символами.
- `Post.title` ограничен 120 символами.
- `Post.body` ограничен 1000 символами.

### Защита от XSS

Пользовательский контент экранируется перед возвратом из API. В `PostController` метод `toResponse(...)` применяет `HtmlUtils.htmlEscape(...)` к заголовку поста, телу поста и имени автора. Поэтому строка вроде:

```html
<script>alert(1)</script>
```

возвращается клиенту в безопасном виде:

```html
&lt;script&gt;alert(1)&lt;/script&gt;
```

Это проверяется тестом `createdPostIsEscapedInApiResponse`.

### Аутентификация и авторизация

Аутентификация реализована через JWT:

- Публичным является только `POST /auth/login` и стандартный `/error`.
- Все остальные эндпоинты требуют заголовок `Authorization: Bearer <JWT>`.
- `JwtAuthenticationFilter` извлекает Bearer-токен, проверяет подпись и срок действия через `JwtService`, после чего создает authentication в `SecurityContext`.
- JWT подписывается алгоритмом HS256 и содержит `subject`, `issuer`, `issuedAt`, `expiresAt` и список ролей.
- Время жизни токена задается настройкой `app.jwt.ttl-seconds`, сейчас это 3600 секунд.

Пароли не хранятся в открытом виде. Seed-пользователь сохраняется с хешем BCrypt через `BCryptPasswordEncoder(12)`, а при логине пароль проверяется методом `passwordEncoder.matches(...)`.

Сессии отключены:

```java
SessionCreationPolicy.STATELESS
```

Это означает, что сервер не хранит пользовательскую HTTP-сессию, а каждый защищенный запрос должен содержать валидный JWT.

### Дополнительные настройки безопасности

- CSRF отключен, потому что API stateless и использует Bearer JWT, а не cookie-сессии.
- H2 Console отключена через `spring.h2.console.enabled=false`.
- Для неавторизованных запросов настроен единый ответ `401 Unauthorized`.
- Входные DTO используют Bean Validation: `@NotBlank` и `@Size`.
- Включены security headers через Spring Security headers configuration.

## CI/CD и security reports

Workflow находится в `.github/workflows/ci.yml` и запускается на `push` и `pull_request`.

Этапы pipeline:

1. `./gradlew test` - запуск тестов.
2. `./gradlew spotbugsMain` - SAST-проверка Java-кода через SpotBugs.
3. `./gradlew dependencyCheckAnalyze` - SCA-проверка зависимостей через OWASP Dependency-Check.
4. Загрузка HTML/JSON-отчетов как GitHub Actions artifact `security-reports`.

Если GitHub Actions падает с ошибкой:

```text
./gradlew: Permission denied
```

значит у Gradle wrapper не был выставлен executable bit в git. Исправление:

```bash
git update-index --chmod=+x gradlew
git commit -m "Fix Gradle wrapper executable permission"
```

После успешного запуска CI/CD нужно открыть раздел `Actions`, выбрать последний успешный workflow run и скачать artifact `security-reports`.

Места для скриншотов, которые нужно вставить после запуска Actions:

![Successful GitHub Actions run](docs/screenshots/actions-success.png)

![SpotBugs SAST report](docs/screenshots/spotbugs-report.png)

![OWASP Dependency-Check SCA report](docs/screenshots/dependency-check-report.png)

## Как запустить локально и проверить в Postman

### 1. Запустить приложение

Windows:

```powershell
.\gradlew.bat bootRun
```

Linux/macOS:

```bash
./gradlew bootRun
```

Дождитесь строки в логах о старте приложения на порту `8080`.

### 2. Получить JWT в Postman

Создайте запрос:

```text
POST http://localhost:8080/auth/login
```

Headers:

```text
Content-Type: application/json
```

Body -> raw -> JSON:

```json
{
  "username": "admin",
  "password": "local-demo-password"
}
```

В ответе скопируйте значение поля `token`.

### 3. Проверить защищенный GET-запрос

Создайте запрос:

```text
GET http://localhost:8080/api/data
```

На вкладке Authorization выберите:

```text
Type: Bearer Token
Token: <скопированный JWT>
```

Ожидаемый результат: `200 OK` и JSON-массив постов.

Для проверки защиты удалите токен и повторите запрос. Ожидаемый результат: `401 Unauthorized`.

### 4. Создать пост

Создайте запрос:

```text
POST http://localhost:8080/api/posts
```

Authorization:

```text
Type: Bearer Token
Token: <скопированный JWT>
```

Headers:

```text
Content-Type: application/json
```

Body -> raw -> JSON:

```json
{
  "title": "Postman check",
  "body": "Created from Postman"
}
```

Ожидаемый результат: `201 Created` и JSON созданного поста.

### 5. Проверить XSS-защиту

Отправьте `POST /api/posts` с телом:

```json
{
  "title": "<script>alert(1)</script>",
  "body": "Hello <b>world</b>"
}
```

Ожидаемый результат: HTML-теги вернутся экранированными, например `&lt;script&gt;alert(1)&lt;/script&gt;`.

## Локальные проверки

```bash
./gradlew test
./gradlew spotbugsMain
./gradlew dependencyCheckAnalyze
```

На Windows используйте `.\gradlew.bat`:

```powershell
.\gradlew.bat test
.\gradlew.bat spotbugsMain
.\gradlew.bat dependencyCheckAnalyze
```

Отчеты после локального запуска:

- SpotBugs: `build/reports/spotbugs/main.html`
- OWASP Dependency-Check: `build/reports/dependency-check-report.html`

## Ссылки для сдачи

- Репозиторий: `https://github.com/yaroslavesev/cyber-security-course`
- Последний успешный pipeline: `<добавьте ссылку на успешный запуск Actions/CI>`
