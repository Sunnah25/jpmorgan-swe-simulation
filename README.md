# JPMorgan Chase Software Engineering Simulation

A backend financial transaction processing system built as part of the JPMorgan Chase Advanced Software Engineering virtual experience on Forage.

## What This Project Does

Midas Core is a microservice that processes financial transactions in real time. When a user sends money to another user:

1. The transaction arrives via a **Kafka message queue**
2. The system validates the sender exists and has sufficient funds
3. An external **Incentive API** is called to check for any bonus rewards
4. Balances are updated in the **database**
5. The updated balance can be queried via a **REST API**

This mirrors the architecture used in real fintech backend systems.

## Architecture

```
┌─────────────────┐     ┌───────────────┐     ┌──────────────────┐
│  Kafka Producer │────▶│  Kafka Topic  │────▶│TransactionListener│
│  (Test Client)  │     │ (transactions)│     │  @KafkaListener  │
└─────────────────┘     └───────────────┘     └────────┬─────────┘
                                                        │
                                              ┌─────────▼─────────┐
                                              │  Validate sender  │
                                              │  has enough funds │
                                              └─────────┬─────────┘
                                                        │
                              ┌─────────────────────────┼──────────────────┐
                              │                         │                  │
                    ┌─────────▼──────┐       ┌──────────▼──────┐          │
                    │  Incentive API │       │   H2 Database   │          │
                    │  (port 8080)   │       │  UserRecord     │          │
                    │  POST/incentive│       │  (JPA/Hibernate)│          │
                    └─────────┬──────┘       └──────────┬──────┘          │
                              │                         │                  │
                              └──────────────┬──────────┘                  │
                                             │                             │
                                    ┌────────▼────────┐                   │
                                    │ Update balances │                   │
                                    │ Save to DB      │                   │
                                    └─────────────────┘                   │
                                                                           │
                                                              ┌────────────▼──────┐
                                                              │  REST API         │
                                                              │  GET /balance     │
                                                              │  (port 33400)     │
                                                              └───────────────────┘
```

## Tech Stack

| Technology | Purpose |
|------------|---------|
| Java 17 | Core language |
| Spring Boot 3.2 | Application framework |
| Apache Kafka | Event-driven message queue for transaction processing |
| Spring Data JPA | Database access layer |
| H2 Database | In-memory database for development and testing |
| Spring Web (RestTemplate) | HTTP client for calling external Incentive API |
| Maven | Build tool and dependency management |
| JUnit 5 | Automated testing |

## Project Structure

```
src/main/java/com/jpmc/midascore/
├── MidasCoreApplication.java      # Entry point
├── component/
│   ├── TransactionListener.java   # Kafka consumer - core business logic
│   ├── DatabaseConduit.java       # Database helper
│   └── BalanceController.java     # REST API endpoint
├── entity/
│   └── UserRecord.java            # Database table mapping
├── repository/
│   └── UserRepository.java        # Database operations
└── foundation/
    ├── Transaction.java           # Transaction data model
    ├── Balance.java               # Balance response model
    └── IncentiveResponse.java     # Incentive API response model
```

## How to Run

### Prerequisites
- Java 17
- Maven (or use the included `mvnw` wrapper)
- The `transaction-incentive-api.jar` (included in `/services`)

### Start the Incentive API
```bash
java -jar services/transaction-incentive-api.jar
```

### Run the tests
```bash
./mvnw.cmd test -Dtest=TaskFiveTests
```

### Run all tests
```bash
./mvnw.cmd test
```

## What I Built — Task by Task

### Task 1 — Project Setup
Diagnosed and fixed a broken Maven build. Added missing Spring Boot, JPA, Kafka, and H2 dependencies to `pom.xml`. Fixed a `javax` → `jakarta` import breaking change from Spring Boot 3. Configured `application.yml` with database, Kafka, and server settings.

**Key learning:** How Maven manages dependencies, why Spring Boot 3 requires `jakarta` instead of `javax`, and how `application.yml` drives app configuration.

### Task 2 — Kafka Integration
Built a `TransactionListener` using `@KafkaListener` to consume transaction messages from a Kafka topic. Configured JSON serialisation so Java objects are automatically converted to/from JSON when passing through Kafka.

**Key learning:** Event-driven architecture, Kafka producers and consumers, message serialisation, and why banks use queuing systems for high-volume transaction processing.

### Task 3 — Database Integration
Extended the `TransactionListener` to perform real business logic — looking up users in the H2 database via Spring Data JPA, validating the sender has sufficient funds, updating both user balances, and persisting the changes.

**Key learning:** JPA entities and repositories, Spring Data's automatic SQL generation, the `Optional` pattern, and how banking validation logic works.

### Task 4 — Consuming an External REST API
Integrated with the `transaction-incentive-api` service using `RestTemplate` to call a POST endpoint and retrieve bonus incentive amounts for qualifying transactions. Bonus amounts are added to the recipient's balance on top of the transaction amount.

**Key learning:** REST API calls between microservices, `RestTemplate`, HTTP headers and request bodies, and the microservices communication pattern used in enterprise fintech.

### Task 5 — Exposing a REST API
Built a `BalanceController` using `@RestController` and `@GetMapping` to expose a `GET /balance?userId=X` endpoint on port 33400. Other services can now query user balances in real time.

**Key learning:** Building REST endpoints with Spring MVC, `@RequestParam`, automatic JSON serialisation of return values, and how services expose data to the outside world.

## Key Concepts Demonstrated

- **Event-driven architecture** — services communicate via Kafka rather than direct calls
- **Microservices** — small focused services (Midas Core + Incentive API) working together
- **RESTful APIs** — both consuming (Incentive API) and producing (`/balance` endpoint)
- **Data persistence** — JPA/Hibernate mapping Java objects to database tables
- **Financial validation** — balance checks before processing transactions
