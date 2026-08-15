# 🏛️ Olympus

![Java](https://img.shields.io/badge/Java-25-orange.svg)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0.6-brightgreen.svg)
![React](https://img.shields.io/badge/React-18-61dafb.svg)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791.svg)
![AI](https://img.shields.io/badge/AI-Spring%20AI%202.0%20%C2%B7%20Mistral%20%2B%20Gemini-ffb779.svg)

Nutrition tracking — calories, macros, bodyweight — with a conversational agent that can log meals,
build presets and draft weekly plans on request. It is the nutrition half of
[Chiron](https://github.com/Kronos131-dev/Chiron), the training tracker, which reads Olympus data
once the two accounts are linked.

Describe a meal in plain language and the agent estimates its weight and macros; ask *"how much
protein yesterday?"* and it reads your journal.

---

## Three components

| Folder | What it is | Deployed |
|--------|------------|----------|
| `olympus-back/` | the Spring Boot REST API | **yes** — by Chiron's pipeline |
| `olympus-pwa/` | the React front, an installable PWA on its own subdomain | **yes** — by Chiron's pipeline |
| `OlympusFront/` | a native Android client (Gradle, Java) | **no** — no pipeline builds it |

The Android client is committed here but is not part of the deployment chain, and is not covered by
any test suite. Treat it as a side experiment until proven otherwise.

---

## What it does

**Food journal.** Daily logs with per-entry quantities, totals recomputed from the entries rather
than accumulated — so deleting an entry can never leave phantom calories behind.

**Food search.** Four sources, tracked per item: text search over the French **CIQUAL** table,
barcode lookup on **Open Food Facts**, manual entry, and AI estimation.

**Meal presets.** Named recipes made of ingredients and quantities, loggable in one action.

**Meal plans.** Weekly planning with recurrence rules, evaluated per day of week.

**Analytics.** Intake against goals, macro balance, and bodyweight evolution over time.

**AI estimation.** `AiService` sends a described meal to Mistral through Spring AI and converts the
answer into a structured estimation — name, total weight, kcal, proteins, carbs, fats.

**The Oracle.** A separate, hand-written agent layer (`service/ai/`) with its own tool loop:
`get_daily_log`, `log_estimated_food`, `create_meal_preset`, `create_meal_plan`, `get_user_profile`
and more. It runs on Mistral or Gemini, with a rate limiter sized for a free Mistral key (~1 req/s),
a retry policy, and a cap on tool round-trips per message.

**Accounts.** JWT authentication with refresh tokens and password reset by email, plus an
integration link that lets Chiron read a user's nutrition through a permanent token.

---

## Architecture

`olympus-back` — Java 25, Spring Boot 4.0.6, Spring Data JPA, Spring Security, PostgreSQL with
Flyway (V1 → V8), MapStruct, jjwt 0.12, springdoc OpenAPI. Serves on port **8080**.

Two AI paths coexist deliberately:

- **Spring AI 2.0** (`service/AiService`) for one-shot structured estimation, using `ChatModel`,
  `PromptTemplate` and `BeanOutputConverter`;
- **a hand-written agent** (`service/ai/`) for the conversational loop, because it needs explicit
  control over tool rounds, provider switching and rate limiting.

`olympus-pwa` — React 18, Vite 5, TypeScript 5.6, Tailwind 4, `vite-plugin-pwa`. Screens: dashboard,
add food, meals and meal editor, weekly plan, Oracle, profile, and the auth flows. The visual
language is documented in [`DESIGN.md`](DESIGN.md).

`AGENT.md` holds the product brief and the entity model.

---

## Running it locally

**Prerequisites** — JDK 25, Node 22, Docker, and a Mistral API key.

```bash
# 1. Database — postgres:16-alpine on host port 5433
cd olympus-back && docker compose up -d olympus-db

# 2. Backend — http://localhost:8080
MISTRAL_API_KEY=<your key> ./mvnw spring-boot:run

# 3. Frontend
cd ../olympus-pwa && npm install && npm run dev
```

Flyway applies V1 → V8 on an empty schema. Note that `ddl-auto` is `update` in the default profile:
the migrations do **not** fully describe the schema, and Hibernate completes what they leave out.
An Olympus database therefore cannot be rebuilt from the migrations alone — worth knowing before
relying on them.

### Configuration

| Variable | Purpose | Required |
|----------|---------|----------|
| `MISTRAL_API_KEY` | Mistral, for estimation and the agent | **yes** |
| `SPRING_DATASOURCE_URL` / `_USERNAME` / `_PASSWORD` | database connection | defaults to localhost:5433 |
| `GEMINI_API_KEY` | enables Gemini for the agent; blank means Mistral only | no |
| `OLYMPUS_AGENT_MISTRAL_MODEL` / `_GEMINI_MODEL` | model names | no |
| `OLYMPUS_AGENT_MAX_TOOL_ROUNDS` | tool round-trips per message, default 3 | no |
| `OLYMPUS_AGENT_MISTRAL_MIN_INTERVAL_MS` / `_GEMINI_MIN_INTERVAL_MS` | minimum spacing between LLM calls, default 1100 ms | no |
| `OLYMPUS_AGENT_LLM_MAX_RETRIES` | retries on a failing LLM call, default 3 | no |
| `GMAIL_USERNAME` / `GMAIL_APP_PASSWORD` / `MAIL_HOST` / `MAIL_PORT` | password-reset mail | no |
| `FRONTEND_URL` | base URL used in outgoing links | no |

---

## Tests

```bash
cd olympus-back
./mvnw test        # 43 tests
```

The test profile disables Flyway and lets Hibernate build the schema with `create-drop`, so the
suite needs a reachable PostgreSQL with the `unaccent` and `pg_trgm` extensions — the CI job creates
them before running.

Coverage is thin relative to the codebase and concentrated where breakage is silent: JWT issuing and
validation, the security filter chain, the AI estimation path, the presence of Flyway's
auto-configuration, and the daily-log service.

---

## Deployment

**`.github/workflows/ci-cd.yml` in this repository runs the tests and deploys nothing.**

Deployment is owned by the [Chiron](https://github.com/Kronos131-dev/Chiron) pipeline, which clones
this repository, builds the backend JAR and the PWA bundle, runs the tests against a PostgreSQL
service container, then installs both onto the server and health-checks them.

The practical consequence: **pushing to `olympus/main` puts nothing into production.** A push to
`Chiron/main` is what ships Olympus. Merge here first — the CI here validates the build in
isolation — then ship from Chiron.
