# my-market-app

![Java](https://img.shields.io/badge/Java-17-informational?logo=java)
![Postgres](https://img.shields.io/badge/PostgreSQL-17-informational?logo=postgresql)
![Docker](https://img.shields.io/badge/Docker-compose-blue?logo=docker)

## О проекте

**my-bank-app-9** <br>
— микросервисное приложение для работы с интернет-магазином:
управление каталогом товаров, корзиной и заказами (`store`) и обработка платежей (`payment`).
Приложение построено с использованием реактивного подхода на Spring WebFlux,
с хранением данных в PostgreSQL и кешированием в Redis.

В приложении реализована авторизация пользователей и межсервисная авторизация.
Для туннелирования при запросах снутри контейнеров изпользуется сервис [ngrok](https://ngrok.com).
Балансировка и проксирование трафика на разные сервисы осуществляется через Nginx.

---

## Структура проекта

```declarative;
my-bank-app/
├── account/
│   ├── main/                 # Application - @SpringBootApplication -jar для сервиса работы со счетом
│   │ └── db/changelog/       # Миграции Liquibase
│   ├── integrationtests/     # Интеграционные и контрактные тесты (Postgres)
│   └── Dockerfile            # Конфигурация Docker для account
├── cash/
│   ├── main/                 # Application - @SpringBootApplication -jar для сервиса пополнения снятия со счета
│   │ └── db/changelog/       # Миграции Liquibase
│   ├── integrationtests/     # Интеграционные и контрактные тесты (Postgres)
│   └── Dockerfile            # Конфигурация Docker для cash
├── documentation/            # Документация по модулям 
├── env/                      # Секреты и настройки для keycloak 
├── front-ui/
│   ├── main/                 # Application - @SpringBootApplication -jar для сервиса фронта
│   ├── integrationtests/     # Интеграционные и контрактные тесты (Postgres)
│   └── Dockerfile            # Конфигурация Docker для front-ui
├── gateway/
│   ├── main/                 # Application - @SpringBootApplication -jar для сервиса гейтвея с фронта
│   ├── integrationtests/     # Интеграционные и контрактные тесты (Postgres)
│   └── Dockerfile            # Конфигурация Docker для gateway
├── microservice-chases/
├── notification/
│   ├── main/                 # Application - @SpringBootApplication -jar для сервиса уведомлений
│   │ └── db/changelog/       # Миграции Liquibase
│   ├── integrationtests/     # Интеграционные тесты (Postgres)
│   └── Dockerfile            # Конфигурация Docker для notification
├── transfer/
│   ├── main/                 # Application - @SpringBootApplication -jar для сервиса переводов
│   │ └── db/changelog/       # Миграции Liquibase
│   ├── integrationtests/     # Интеграционные и контрактные тесты (Postgres)
│   └── Dockerfile            # Конфигурация Docker для transfer
├── docker-compose.yml         # Главный файл оркестрации Docker сервисов
├── README.md
└── settings.gradle
```

---

## Применяемые технологии

- **Java 17** — основная платформа разработки.
- **Spring Boot / WebFlux** — реактивные REST API.
- **Project Reactor** — реактивные потоки (`Mono`, `Flux`).
- **PostgreSQL** — основная база данных для хранения данных `account`, `cash`, `notification`, `transfer`.
- **Liquibase** — управление миграциями базы данных.
- **Docker / Docker Compose** — локальное развёртывание сервисов.
- **Lombok** — генерация boilerplate кода (`@Slf4j`, `@RequiredArgsConstructor`).
- **JUnit 5 / StepVerifier** — тестирование реактивных сервисов.
- **Jacoco** — сбор покрытия тестов.
- **Keycloak** — сервер авторизации.

## Быстрый старт

1. **Подготовка**

- создать БД

```sql;

```

- скопировать приложение

```bash

git clone -b feature/module_two_sprint_eight_branch https://github.com/Habatoo/my-bank-app-9.git
cd my-bank-app-9
```

- сборка и запуск

```bash

docker buildx bake
docker compose up -d 
```

- приложение доступно по адресу https://unmanned-steelless-eliana.ngrok-free.dev/  через ngrok

- авторизация в приложении
  `LOGIN=user`   
  `PASSWORD=password`


2. **Модули приложения:**<br>

- Модуль `payment`
    - Управление платежами и кошельком пользователя.
    - Реализует создание платежей, проверку баланса, статусы SUCCESS / FAILED.
    - Интеграция с локальными сервисами и тестами.
    - Подробнее: [Документация модуля payment](./documentation/payment.md)

- Модуль `store`
    - Управление каталогом товаров, корзиной и заказами.
    - Кеширование через Redis, постоянное хранение в Postgres.
    - Миграции через Liquibase.
    - Подробнее: [Документация модуля store](./documentation/store.md)

| Модуль          | Контроллер/эндпоинт                                                | Контрактные проверки                                                   |
| --------------- | ------------------------------------------------------------------ | ---------------------------------------------------------------------- |
| Accounts        | GET /accounts/{login}, PUT /accounts/{login}, GET /accounts/lookup | Ответы 200/400/403/404, структура JSON, валидация                      |
| Cash            | POST /cash/deposit, POST /cash/withdraw                            | Ответы 200/400, проверка баланса, обязательные поля, Notifications     |
| Transfer        | POST /transfer                                                     | Ответы 200/400/404, проверка баланса, обязательные поля, Notifications |
| Notifications   | POST /notifications                                                | Структура уведомлений, ошибки формата                                  |
| Gateway + Front | Проброс JWT, доступ к микросервисам через Gateway                  | JWT передаётся, коды 200/401/403, структура JSON                       |


## Более расширенные инструкции

- [Работа с БД и миграциями Liquibase](./documentation/database.md)
- [Руководство по деплою и настройкам](./documentation/deploy.md)
- [Получение отчетов jacoco](./documentation/jacoco.md)

---
