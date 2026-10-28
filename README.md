# External APIs Bootcamp - Demo App

This Spring Boot project is the starter for **External APIs in Spring Boot**
(RestClient and WebClient). Students do **not** build this from scratch - the
core (interfaces, config, controller) is already in place, and you
implement the RestClient & WebClient logic behind it.

## Domain

A simple book catalog app that:

- Fetches books from an external book service (blocking and non-blocking).
- Exposes its own REST endpoints on top of that external data.

## Running the app

Open the project in IntelliJ (or your preferred IDE, e.g. VS Code) and run
`ExternalApisBootcampDemoApplication` like any other Spring Boot app - right-click
-> Run, or use the run/play button next to the `main` method.

The app starts on `http://localhost:8080` by default.

## External service

The base URL of the external book service is already configured in
`src/main/resources/application.yaml` (`external-service.base-url`) - you do
not need to set this up yourself.

You can explore the external service's own API directly through its Swagger UI:

**https://external-api.acnbootcamp.lv/swagger-ui.html**

Use it to inspect the exact JSON shape returned by `GET /api/books/{id}` and
`GET /api/books` - you'll need this to define `BookApiResponse` (see below).

## Practical Task 1: RestClient

Required tasks:
- Explore the external service's Swagger UI and define `BookApiResponse` to
  match the raw JSON response for `GET /api/books/{id}`.
- Design the `ClientException` class yourself, to be used for wrapping errors
  from the external service.
- Implement `BookRestClientImpl` using `RestClient.Builder` to fetch one book
  by ID and all books, mapping each `BookApiResponse` to a `BookDto`.
- Handle `RestClient` error cases (client errors, server errors, connection
  issues) and rethrow them as `ClientException`. Verify this against at least
  2-3 of the reserved chaos book IDs (991-993).
- Keep URLs in configuration, not hardcoded in Java.

Success criteria:
- The application can fetch one book through `RestClient`.
- The application can fetch all books through `RestClient`.
- The implementation uses the blocking, synchronous approach correctly.
- External service errors are caught and rethrown as `ClientException` instead
  of leaking framework-specific exceptions.

Bonus:
- Use the rest of the reserved chaos book IDs (99, 994-999) to test your error
  handling against more failure scenarios (timeouts, malformed responses, etc.).

## Practical Task 2: WebClient

Required tasks:
- Design the `ClientException` class yourself, to be used for wrapping errors
  from the external service (shared with Task 1, if not already done).
- Implement `BookWebClientImpl` using `WebClient.Builder` to fetch one book by
  ID and all books reactively, mapping each `BookApiResponse` to a `BookDto`.
- Implement fetching two books in parallel with `Mono.zip()`.
- Handle `WebClient` error cases (response errors, request/connection issues)
  and rethrow them as `ClientException`. Verify this against at least 2-3 of
  the reserved chaos book IDs (991-993).
- Do not use `.block()`.

Success criteria:
- The application can fetch one book through `WebClient`.
- The application can fetch all books through `WebClient`.
- The application can fetch two books in parallel through `WebClient` using
  `Mono.zip()`.
- The implementation uses the non-blocking, asynchronous approach correctly.
- External service errors are caught and rethrown as `ClientException` instead
  of leaking framework-specific exceptions.

Bonus:
- Use the rest of the reserved chaos book IDs (99, 994-999) to test resilience
  (retries, timeouts, error mapping) in a non-blocking way.

## Project Structure

```
src/main/java/com/accenture/externalapis/demo/
├── client/
│   ├── BookRestClient.java          (interface - blocking client seam)
│   ├── BookRestClientImpl.java      (TODO - implement with RestClient)
│   ├── BookWebClient.java           (interface - non-blocking client seam)
│   ├── BookWebClientImpl.java       (TODO - implement with WebClient)
│   └── ClientException.java         (TODO - design yourself)
├── config/
│   ├── ExternalServiceProperties.java   (typed config for external-service.base-url)
│   └── GlobalExceptionHandler.java      (maps ClientException to HTTP error responses)
├── controller/
│   └── BookController.java          (REST endpoints, already implemented)
├── dto/
│   ├── BookApiResponse.java         (TODO - define to match raw external JSON)
│   └── BookDto.java                 (domain DTO, already defined)
└── ExternalApisBootcampDemoApplication.java
```

## Prerequisites

- Java 21
- No external tools needed - the external service is already deployed and
  pre-configured, you don't need to run anything else locally.
