# Cyber Security Course

Учебные работы по курсу безопасности.

## Работы

- [lab1](lab1/) - Secure REST API на Java 17 и Spring Boot с JWT, bcrypt, защитой от XSS, тестами, SpotBugs и OWASP Dependency-Check.

## Локальная проверка

```powershell
cd lab1
.\gradlew.bat test spotbugsMain
```

Для ускорения OWASP Dependency-Check можно задать `NVD_API_KEY` через переменную окружения или GitHub Secrets.
