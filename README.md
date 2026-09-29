# Distributed E-Commerce Microservices

A Spring Boot microservices backend for a small e-commerce catalog and order flow. A REST API gateway translates HTTP/JSON into gRPC calls to three services: catalog (MongoDB), search (Elasticsearch) and orders (PostgreSQL). Product changes reach the search index asynchronously through Kafka. The current code is a deliberately naive baseline: it has the failure modes listed under [Known limitations](#known-limitations), and later work fixes each one with a test and a measured before/after.

## Results

*Filled in as fixes land.* Each row is added in the same change as the fix and the test that proves it. "Before" is measured on the `baseline-dual-write` tag.

| Guarantee | Before (`baseline-dual-write`) | After | Test | Raw output |
|---|---|---|---|---|
| | | | | |

## Known limitations

These are real bugs in the current code. They are kept on purpose as the measured baseline, and each will be fixed with a failing test first.

| Limitation | Where | Planned fix |
|---|---|---|
| **Dual write.** The catalog saves the product to MongoDB, then sends the Kafka event with an unchecked, fire-and-forget `kafkaTemplate.send()`. If Kafka is unavailable, the product exists in MongoDB but never reaches the search index. No transaction covers the save and the send; MongoDB runs standalone with no transaction manager, and a MongoDB transaction could not include the Kafka send anyway. | `CatalogServiceImpl.createProduct` | Transactional outbox + relay (week 3) |
| **Swallowed consumer exceptions.** The search consumer catches every exception and only logs it, so the offset is committed and the event is lost (for example, while Elasticsearch is down). There is no retry and no dead-letter topic. | `KafkaProductConsumer` | Error handler with backoff and a DLT (week 2) |
| **Client-supplied price.** `POST /api/orders` accepts the item price from the client and the order total is computed from it. | `GatewayOrderController`, `OrderGrpcService` | Server-side pricing from the catalog (week 4) |
| **Non-idempotent order creation.** Every `POST /api/orders` inserts a new order, so a client retry creates a duplicate order. | `OrdersServiceImpl.addOrderProducts` | `Idempotency-Key` backed by a unique constraint (week 4) |

## Architecture

```text
┌──────────────┐       HTTP/JSON        ┌───────────────────┐
│              │ ─────────────────────► │                   │
│   Client     │                        │    API Gateway    │
│              │ ◄───────────────────── │   (Spring MVC)    │
│              │                        │    (Port 8081)    │
└──────────────┘                        └─────────┬─────────┘
                                                  │
                                         gRPC     │
          ┌───────────────────────────────────────┼──────────────────────────────────────┐
          │                                       │                                      │
          ▼                                       ▼                                      ▼
┌───────────────────┐                   ┌───────────────────┐                  ┌───────────────────┐
│                   │                   │                   │                  │                   │
│   Order Service   │                   │  Catalog Service  │                  │   Search Service  │
│    (Port 9090)    │                   │    (Port 9091)    │                  │    (Port 9093)    │
│                   │                   │                   │                  │                   │
└─────────┬─────────┘                   └─────────┬─────────┘                  └─────────┬─────────┘
          │                                       │                                      │
          │ JDBC                                  │ Mongo Driver                         │ Spring Data ES
          ▼                                       ▼                                      ▼
    ┌────────────┐                          ┌────────────┐                         ┌─────────────┐
    │ PostgreSQL │                          │  MongoDB   │                         │Elasticsearch│
    └────────────┘                          └─────┬──────┘                         └──────▲──────┘
                                                  │                                       │
                                                  │         ProductCreatedEvent           │
                                                  └───────────────────────────────────────┘
                                                                 (via Kafka)
```

All services register with a Eureka discovery server (port 8761), and the gateway resolves the gRPC services through it.

## Design choices

* **gRPC between the gateway and services.** The `.proto` files give typed contracts, and gRPC has explicit status codes and deadlines. The gateway stays REST/JSON for external clients. No REST-vs-gRPC performance comparison has been run.
* **Kafka between catalog and search.** The catalog publishes a `ProductCreatedEvent` keyed by product ID, and the search service indexes it by the same ID, so replaying an event overwrites the same document instead of creating a new one. Search is eventually consistent with the catalog. The event is not yet delivered reliably (see Known limitations).
* **A database per service.**
  * **PostgreSQL** for orders (relational, transactional).
  * **MongoDB** for the product catalog (flexible per-product attributes).
  * **Elasticsearch** for the search index.

## Services

### API Gateway (port 8081)
* **Tech:** Spring Boot (Spring MVC), gRPC clients.
* **Role:** The only HTTP entry point. Maps REST/JSON requests to gRPC calls on the services.

### Order Service (gRPC port 9090)
* **Tech:** Spring Boot, Spring Data JPA, PostgreSQL.
* **Role:** Creates orders and fetches an order by ID. Orders start in status `Payment Pending`.

### Catalog Service (gRPC port 9091)
* **Tech:** Spring Boot, Spring Data MongoDB, Kafka producer.
* **Role:** Source of truth for products. Saves each new product to MongoDB, then publishes a `ProductCreatedEvent` to the `product-events` topic.

### Search Service (gRPC port 9093)
* **Tech:** Spring Boot, Spring Data Elasticsearch, Kafka consumer.
* **Role:** Consumes `product-events`, upserts each product into Elasticsearch by ID, and serves name/description search.

### Discovery Server (port 8761)
* **Tech:** Netflix Eureka.

## Local development

### Prerequisites
* Java 21 (the Lombok version used does not build on JDK 25+)
* Maven
* Docker and Docker Compose
* Python 3 with `requests` (for sample data)

### 1. Start infrastructure
Starts PostgreSQL, MongoDB, ZooKeeper, Kafka and Elasticsearch:
```bash
docker-compose up -d
```

### 2. Start the discovery server
```bash
cd services/discovery-server
mvn clean package -DskipTests
java -jar target/discovery-server-0.0.1-SNAPSHOT.jar
```

### 3. Start the backend services
Use a separate terminal for each:
```bash
# Catalog Service
cd services/catalog-service
mvn clean package -DskipTests
java -jar target/Catalog-0.0.1-SNAPSHOT.jar

# Search Service
cd services/search-service
mvn clean package -DskipTests
java -jar target/Search-0.0.1-SNAPSHOT.jar

# Order Service
cd services/order-service
mvn clean package -DskipTests
java -jar target/Orders-0.0.1-SNAPSHOT.jar
```

### 4. Start the API gateway
```bash
cd services/api-gateway
mvn clean package -DskipTests
java -jar target/ecomProject-0.0.1-SNAPSHOT.jar
```

### 5. Load sample data
`hydrate.py` posts products through the gateway. It reads Amazon product CSVs from `./dataset/`, which is not committed, and currently loads 5 products from `amazon_laptop.csv`.
```bash
python3 hydrate.py
```

## API endpoints (gateway)

* `POST /api/products`: create a product (publishes a Kafka event)
* `GET /api/products/{id}`: get a product from the catalog
* `GET /api/search?q=`: search products by name or description
* `POST /api/orders`: create an order
* `GET /api/orders/{id}`: get an order

## Demo UI (optional)

`frontend/` is a small Vue 2 app for browsing products. It calls two gateway endpoints: `GET /api/search?q=` for the product list and search, and `GET /api/products/{id}` for the product detail page. It has no cart, checkout or login.

```bash
cd frontend
npm install && npm run serve
```

It expects the gateway on `http://localhost:8081` (override with `VUE_APP_API_BASE_URL`). It is a thin browsing client with no tests, and it plays no part in the guarantees or the Results table.

## License

[MIT](LICENSE)
