# Sentry Pay

A payment processing API built with Java, Spring Boot, MySQL, Redis, and Stripe. Handles idempotent payment creation and reliable processing of out-of-order refund webhooks.

## Tech Stack

**Backend:** Java, Spring Boot, Spring Data JPA, Redis, Stripe API, MySQL  
**Frontend:** React, TypeScript, Material UI, Stripe.js  
**Tools:** Docker, Maven, Git

## Features

- **Idempotent Payment Creation** — every payment request requires an `Idempotency-Key` header. Keys are cached in Redis with a 24-hour TTL, so network retries return the original cached result instead of creating a duplicate charge with Stripe.
- **Out-of-Order Webhook Handling** — refund webhooks that arrive before their parent charge exists in the database are stored as PENDING instead of being dropped. A scheduled job retries pending events every 10 seconds until the parent charge is found or the retry limit is reached.
- **Stripe Integration** — real PaymentIntent creation and card confirmation using Stripe's test environment.
- **Live Idempotency Demo** — the frontend lets you resend the exact same payment request with the same idempotency key, visually showing the cached response instead of a duplicate Stripe charge.

## How It Works

### Idempotent Payment Flow

```
Client sends payment request with Idempotency-Key header
        ↓
Check Redis for existing key
        ↓
Found in Redis → return cached payment immediately (no Stripe call)
        ↓
Not in Redis → check database as fallback
        ↓
Not in database either → create new PaymentIntent with Stripe
        ↓
Save payment, cache idempotency key in Redis (24h TTL)
```

### Out-of-Order Webhook Retry Flow

```
Refund webhook arrives from Stripe
        ↓
Check if parent charge exists in database
        ↓
Exists → mark payment REFUNDED, mark event PROCESSED
        ↓
Doesn't exist yet → save event as PENDING
        ↓
Scheduled job runs every 10 seconds
        ↓
Retries all PENDING events, checking again for the parent charge
        ↓
Succeeds once the charge exists, or gives up after 5 retries
```

## Running Locally

### Prerequisites
- Java 17+
- Maven
- Docker
- Node.js 18+
- A free Stripe account (test mode)

### Backend

1. Start MySQL with Docker:
```bash
docker run --name sentry-pay-db -e MYSQL_ROOT_PASSWORD=password -e MYSQL_DATABASE=sentry_pay -p 3308:3306 -d mysql:8
```

2. Start Redis with Docker:
```bash
docker run --name sentry-pay-redis -p 6380:6379 -d redis:7
```

3. Configure `application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3308/sentry_pay?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=password
spring.data.redis.host=localhost
spring.data.redis.port=6380
server.port=8082
stripe.api.secret-key=your_stripe_secret_key
```

4. Run the Spring Boot app:
```bash
./mvnw spring-boot:run
```

### Frontend

Update the Stripe publishable key in `CheckoutPage.tsx`, then:
```bash
cd frontend
npm install
npm start
```

App runs at `http://localhost:3000`  
API runs at `http://localhost:8082`

Test card: `4242 4242 4242 4242`, any future expiry, any CVC, any ZIP.

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|--------------|
| POST | `/api/payments` | Create a payment (requires `Idempotency-Key` header) |
| POST | `/api/webhooks/refund` | Receive a refund webhook event |