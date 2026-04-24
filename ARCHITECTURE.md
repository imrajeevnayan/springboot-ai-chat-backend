# OpenRouter Chat Backend

## Understanding Summary

- Build a ChatGPT-like backend with no UI using Java 21, Spring Boot 3.x, Maven, JPA, and OpenRouter.
- Expose stateless REST APIs where every request carries the caller's `userId`.
- Persist users, conversations, and messages through JPA; local development uses file-backed H2.
- Keep layering explicit: Controller -> Service -> Repository, with OpenRouter behind a provider client.
- Handle validation, provider failures, retries, timeouts, and rate limits.
- Keep the implementation provider-aware but not provider-entangled so another LLM API can be added later.

## Assumptions

- Authentication is handled by an upstream gateway or future Spring Security integration; this service currently treats `userId` as caller identity.
- Conversation context is bounded to the most recent configured messages to control latency and token cost.
- Failed provider calls persist the user prompt but do not create an assistant message.
- The default `local` profile uses file-backed H2 so the application boots without a local PostgreSQL server and retains development data across restarts.
- Database migrations should be managed with Flyway or Liquibase in production; `src/main/resources/db/schema.sql` documents the PostgreSQL/MySQL schema baseline.

## Components

- `ChatController`: HTTP API contract, request validation, status codes.
- `ChatService`: prompt orchestration, user/conversation lookup, context assembly, persistence.
- `OpenRouterClient`: LLM integration, timeout, retry, response validation, provider error mapping.
- `RateLimitService`: basic in-memory per-user request limiter.
- `Repository` interfaces: Spring Data JPA persistence.
- `RestExceptionHandler`: consistent JSON error envelope.

## Data Flow

1. Client sends `POST /api/v1/chat/messages` with `userId`, prompt, optional `conversationId`, and optional model.
2. Controller validates the DTO.
3. Service checks the per-user rate limit.
4. Service gets or creates the user and conversation.
5. User message is stored.
6. Recent conversation messages are loaded and converted to OpenRouter's chat message format.
7. OpenRouter is called with timeout and retry policy.
8. Assistant message is stored and returned to the client.

## REST API

### Send Chat Message

`POST /api/v1/chat/messages`

Request:

```json
{
  "userId": "user-123",
  "conversationId": "6d210bbf-3774-4cc5-8a34-6533ec1481ae",
  "title": "Architecture planning",
  "prompt": "Design a scalable chat backend",
  "model": "openai/gpt-4o-mini"
}
```

`conversationId` is optional. If omitted, a new conversation is created.

Response `201 Created`:

```json
{
  "conversationId": "6d210bbf-3774-4cc5-8a34-6533ec1481ae",
  "userMessageId": "c7f0a715-39db-4a01-92f7-b64b1307d732",
  "assistantMessageId": "3aa4792f-f5db-4f79-9cb2-b3db807e033a",
  "role": "assistant",
  "content": "A scalable design would...",
  "model": "openai/gpt-4o-mini",
  "createdAt": "2026-04-24T07:30:00Z"
}
```

### List Conversations

`GET /api/v1/users/{userId}/conversations?page=0&size=20`

Response `200 OK`:

```json
{
  "items": [
    {
      "id": "6d210bbf-3774-4cc5-8a34-6533ec1481ae",
      "title": "Architecture planning",
      "createdAt": "2026-04-24T07:00:00Z",
      "updatedAt": "2026-04-24T07:30:00Z"
    }
  ],
  "totalItems": 1,
  "page": 0,
  "size": 20,
  "totalPages": 1
}
```

### List Messages

`GET /api/v1/conversations/{conversationId}/messages?userId=user-123&page=0&size=50`

Response `200 OK` returns paginated messages in ascending creation order.

### Delete Conversation

`DELETE /api/v1/conversations/{conversationId}?userId=user-123`

Response `204 No Content`.

## Status Codes

- `201`: assistant response created.
- `200`: read request succeeded.
- `204`: conversation deleted.
- `400`: malformed request or invalid ownership parameter.
- `404`: conversation not found for the user.
- `422`: DTO validation failed.
- `429`: local per-user rate limit exceeded.
- `502`: OpenRouter returned an invalid response or non-retryable provider error.
- `503`: OpenRouter rate limited the backend after retries.
- `504`: OpenRouter timeout.

## Database Schema

Tables:

- `users`: internal UUID primary key, unique `external_user_id`, timestamps.
- `conversations`: UUID primary key, `user_id` foreign key, title, timestamps.
- `messages`: UUID primary key, `conversation_id` foreign key, role, content, model, created timestamp.

Indexes:

- `users.external_user_id` unique for identity lookup.
- `conversations(user_id, updated_at)` for conversation listing.
- `messages(conversation_id, created_at)` for context and transcript retrieval.

## Runtime Profiles

- Default profile: `local`, using file-backed H2 at `./data/chat_backend` and Hibernate `update`. This prevents local startup failures when PostgreSQL is not running on `localhost:5432` and keeps chat history between restarts.
- PostgreSQL profile: run with `--spring.profiles.active=postgres` and provide `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.
- MySQL support is available through the included MySQL driver; add an `application-mysql.yml` with the same datasource shape if MySQL is the deployment target.

## Design Decisions

- Use REST instead of GraphQL because the required operations are resource-oriented and simple.
- Use DTO records at the boundary to decouple API shape from JPA entities.
- Keep OpenRouter isolated behind `OpenRouterClient` so provider headers, retries, and response parsing do not leak into domain services.
- Persist messages before and after provider calls to maintain a durable audit trail of accepted prompts.
- Bound context by message count because unbounded transcripts increase latency, cost, and provider failure risk.
- Use UUID primary keys to avoid enumeration risk and ease future horizontal scaling.

## Failure Handling

- Provider 5xx, 429, network failures, and timeouts are retry candidates.
- Empty provider responses become `502 Bad Gateway`.
- Local rate limit failures become `429 Too Many Requests`.
- Global error responses use a stable envelope: `error`, `message`, `details`, `timestamp`, and `path`.

## Security Review

- `OPENROUTER_API_KEY` is read from environment-backed configuration and is never returned in API responses.
- Request DTO validation limits prompt, model, title, and user ID length.
- Conversation reads and deletes verify ownership through `conversationId + userId` repository queries.
- SQL injection risk is low because Spring Data JPA parameter binding is used.
- Production should add real authentication, authorization, TLS-only deployment, secret-manager backed API keys, structured audit logs, and distributed rate limiting.

## Scalability Notes

- Replace the in-memory rate limiter with Redis or gateway-level throttling when running multiple app instances.
- Add Flyway or Liquibase for migrations.
- Add a provider abstraction if supporting multiple LLM vendors.
- Add async job processing or streaming endpoints for long-running model responses.
- Add message token counting and summarization for long conversations.
