# Transfer Service

This is the Transfer Service, a Spring Boot microservice responsible for orchestrating money transfers between accounts. It is designed to work in a microservices architecture alongside the Ledger Service, with both services managed by a shared `docker-compose.yml` at the project root.

## Features
- **POST /transfers**: Initiate a transfer between accounts. Requires an `Idempotency-Key` header for safe retries.
- **GET /transfers/{id}**: Fetch the status of a transfer by its server-assigned transferId.
- **POST /transfers/batch**: Process up to 20 transfers concurrently, each with its own idempotency key.
- **Idempotency**: Ensures repeated requests with the same key do not double-charge.
- **Concurrency**: Batch processing uses parallel execution for performance.
- **Resilience**: Circuit breaker protects against ledger-service failures.
- **Validation**: Input validation and meaningful error responses.

## Running Locally

### Prerequisites
- Docker and Docker Compose
- Java 21 and Maven (for local builds)

### Build the Service
```sh
cd transfer-service
mvn clean package
```

### Start All Services (from project root)
```sh
docker-compose up --build
```
This will start:
- `transfer-service` (on port 8080)
- `ledger-service` (on port 8081)
- Separate Postgres databases for each service

### API Usage
- **POST /transfers**: Initiate a transfer
- **GET /transfers/{id}**: Get transfer status
- **POST /transfers/batch**: Batch transfer (see API docs for details)

### Idempotency
Clients must generate and provide a unique `Idempotency-Key` header for each transfer request. The service will return the same result for repeated requests with the same key.

### Development Notes
- The shared `docker-compose.yml` is at the project root and manages both services and their databases.
- Each service has its own database and does not access the other's tables.
- For local development, you can build and test each service independently with Maven.

### CI
A minimal GitHub Actions workflow is provided in `.github/workflows/ci.yml` to build and test the service on push/PR.

### License
MIT

