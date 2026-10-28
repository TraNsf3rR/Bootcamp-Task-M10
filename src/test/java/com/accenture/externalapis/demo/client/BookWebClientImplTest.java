package com.accenture.externalapis.demo.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookDto;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.codec.DecodingException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.test.StepVerifier;

class BookWebClientImplTest {

    private static final String FIRST_BOOK_RESPONSE = """
            {
              "id": 1,
              "title": "The Spaghetti Whisperer",
              "author": "Sir Reginald Fluffington III",
              "genre": "Software Engineering",
              "price": 34.99,
              "isbn": "978-0132350884",
              "publishedYear": 2008
            }
            """;

    private static final String SECOND_BOOK_RESPONSE = """
            {
              "id": 2,
              "title": "Reactive Recipes",
              "author": "Ada Example",
              "genre": "Technology",
              "price": 19.5,
              "isbn": "978-0123456789",
              "publishedYear": 2024
            }
            """;

    private StubServer server;
    private BookWebClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new StubServer();
        client = new BookWebClientImpl(
                WebClient.builder(),
                new ExternalServiceProperties(server.baseUrl()));
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void getBookAsyncFetchesAndMapsSingleBook() {
        server.respond("/api/books/1", FIRST_BOOK_RESPONSE);

        StepVerifier.create(client.getBookAsync(1L))
                .expectNext(new BookDto(
                        "The Spaghetti Whisperer",
                        "Sir Reginald Fluffington III",
                        "Software Engineering",
                        34.99))
                .verifyComplete();

        assertEquals(Set.of("/api/books/1"), server.requestedPaths());
    }

    @Test
    void getAllBooksAsyncFetchesAndMapsEveryBook() {
        server.respond("/api/books", "[" + FIRST_BOOK_RESPONSE + "," + SECOND_BOOK_RESPONSE + "]");

        StepVerifier.create(client.getAllBooksAsync())
                .expectNext(
                        new BookDto(
                                "The Spaghetti Whisperer",
                                "Sir Reginald Fluffington III",
                                "Software Engineering",
                                34.99),
                        new BookDto("Reactive Recipes", "Ada Example", "Technology", 19.5))
                .verifyComplete();

        assertEquals(Set.of("/api/books"), server.requestedPaths());
    }

    @Test
    void getBooksInParallelReturnsBooksInRequestOrder() {
        server.respond("/api/books/1", FIRST_BOOK_RESPONSE);
        server.respond("/api/books/2", SECOND_BOOK_RESPONSE);

        StepVerifier.create(client.getBooksInParallel(1L, 2L))
                .expectNext(List.of(
                        new BookDto(
                                "The Spaghetti Whisperer",
                                "Sir Reginald Fluffington III",
                                "Software Engineering",
                                34.99),
                        new BookDto("Reactive Recipes", "Ada Example", "Technology", 19.5)))
                .verifyComplete();

        assertEquals(Set.of("/api/books/1", "/api/books/2"), server.requestedPaths());
    }

    @Test
    void wrapsHttpResponseErrors() {
        server.respond("/api/books/991", 404, "not found");
        server.respond("/api/books", 503, "service unavailable");

        StepVerifier.create(client.getBookAsync(991L))
                .expectErrorSatisfies(exception -> assertClientException(
                        exception,
                        WebClientResponseException.class,
                        "404"))
                .verify();
        StepVerifier.create(client.getAllBooksAsync())
                .expectErrorSatisfies(exception -> assertClientException(
                        exception,
                        WebClientResponseException.class,
                        "503"))
                .verify();
    }

    @Test
    void parallelFetchPropagatesClientException() {
        server.respond("/api/books/991", 404, "not found");
        server.respond("/api/books/2", SECOND_BOOK_RESPONSE);

        StepVerifier.create(client.getBooksInParallel(991L, 2L))
                .expectErrorSatisfies(exception -> assertClientException(
                        exception,
                        WebClientResponseException.class,
                        "404"))
                .verify();
    }

    @Test
    void wrapsRequestErrors() {
        server.stop();

        StepVerifier.create(client.getBookAsync(1L))
                .expectErrorSatisfies(exception -> assertClientException(
                        exception,
                        WebClientRequestException.class,
                        "Could not access"))
                .verify();
    }

    @Test
    void wrapsTimeouts() {
        server.respond("/api/books/1", 200, FIRST_BOOK_RESPONSE, Duration.ofSeconds(6));

        StepVerifier.create(client.getBookAsync(1L))
                .expectErrorSatisfies(exception -> assertClientException(
                        exception,
                        TimeoutException.class,
                        "timed out"))
                .verify();
    }

    @Test
    void wrapsMalformedResponses() {
        server.respond("/api/books/1", 200, "{invalid json");

        StepVerifier.create(client.getBookAsync(1L))
                .expectErrorSatisfies(exception -> assertClientException(
                        exception,
                        DecodingException.class,
                        "decode"))
                .verify();
    }

    @Test
    void rejectsEmptyResponses() {
        server.respond("/api/books/1", 200, "");

        StepVerifier.create(client.getBookAsync(1L))
                .expectErrorSatisfies(exception -> {
                    ClientException clientException = assertInstanceOf(ClientException.class, exception);
                    assertTrue(clientException.getMessage().contains("empty response"));
                })
                .verify();
    }

    private static void assertClientException(
            Throwable exception,
            Class<? extends Throwable> causeType,
            String messageFragment) {
        ClientException clientException = assertInstanceOf(ClientException.class, exception);
        assertInstanceOf(causeType, clientException.getCause());
        assertTrue(clientException.getMessage().contains(messageFragment));
    }

    private static final class StubServer {

        private final HttpServer httpServer;
        private final ExecutorService executor = Executors.newCachedThreadPool();
        private final Map<String, StubResponse> responses = new ConcurrentHashMap<>();
        private final Set<String> requestedPaths = ConcurrentHashMap.newKeySet();
        private boolean stopped;

        private StubServer() throws IOException {
            httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            httpServer.setExecutor(executor);
            httpServer.createContext("/api/books", this::handleRequest);
            httpServer.start();
        }

        private String baseUrl() {
            return "http://127.0.0.1:" + httpServer.getAddress().getPort() + "/api";
        }

        private void respond(String path, String body) {
            respond(path, 200, body, Duration.ZERO);
        }

        private void respond(String path, int status, String body) {
            respond(path, status, body, Duration.ZERO);
        }

        private void respond(String path, int status, String body, Duration delay) {
            responses.put(path, new StubResponse(status, body, delay));
        }

        private Set<String> requestedPaths() {
            return Set.copyOf(requestedPaths);
        }

        private void handleRequest(HttpExchange exchange) throws IOException {
            try {
                String path = exchange.getRequestURI().getPath();
                requestedPaths.add(path);
                StubResponse response = responses.getOrDefault(
                        path,
                        new StubResponse(404, "not found", Duration.ZERO));
                if (!response.delay().isZero()) {
                    Thread.sleep(response.delay().toMillis());
                }
                byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(response.status(), body.length);
                try (OutputStream responseBody = exchange.getResponseBody()) {
                    responseBody.write(body);
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        }

        private void stop() {
            if (!stopped) {
                httpServer.stop(0);
                executor.shutdownNow();
                stopped = true;
            }
        }
    }

    private record StubResponse(int status, String body, Duration delay) {
    }
}
