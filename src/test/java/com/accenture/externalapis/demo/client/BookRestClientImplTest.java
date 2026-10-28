package com.accenture.externalapis.demo.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

class BookRestClientImplTest {

    private static final String SINGLE_BOOK_RESPONSE = """
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

    private StubServer server;
    private BookRestClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new StubServer();
        client = new BookRestClientImpl(
                RestClient.builder(),
                new ExternalServiceProperties(server.baseUrl()));
    }

    @AfterEach
    void tearDown() {
        server.stop();
    }

    @Test
    void getBookFetchesAndMapsSingleBook() {
        server.respond("/api/books/1", 200, SINGLE_BOOK_RESPONSE);

        assertEquals(new BookDto(
                "The Spaghetti Whisperer",
                "Sir Reginald Fluffington III",
                "Software Engineering",
                34.99), client.getBook(1L));
    }

    @Test
    void getAllBooksFetchesAndMapsEveryBook() {
        String response = """
                [
                  {
                    "id": 1,
                    "title": "The Spaghetti Whisperer",
                    "author": "Sir Reginald Fluffington III",
                    "genre": "Software Engineering",
                    "price": 34.99,
                    "isbn": "978-0132350884",
                    "publishedYear": 2008
                  },
                  {
                    "id": 2,
                    "title": "Reactive Recipes",
                    "author": "Ada Example",
                    "genre": "Technology",
                    "price": 19.5,
                    "isbn": "978-0123456789",
                    "publishedYear": 2024
                  }
                ]
                """;
        server.respond("/api/books", 200, response);

        assertEquals(List.of(
                new BookDto(
                        "The Spaghetti Whisperer",
                        "Sir Reginald Fluffington III",
                        "Software Engineering",
                        34.99),
                new BookDto("Reactive Recipes", "Ada Example", "Technology", 19.5)),
                client.getAllBooks());
    }

    @Test
    void wrapsClientAndServerErrors() {
        server.respond("/api/books/991", 404, "not found");
        server.respond("/api/books", 503, "service unavailable");

        ClientException clientError = assertThrows(ClientException.class, () -> client.getBook(991L));
        ClientException serverError = assertThrows(ClientException.class, client::getAllBooks);

        assertTrue(clientError.getMessage().contains("404"));
        assertInstanceOf(RestClientResponseException.class, clientError.getCause());
        assertTrue(serverError.getMessage().contains("503"));
        assertInstanceOf(RestClientResponseException.class, serverError.getCause());
    }

    @Test
    void wrapsConnectionErrors() {
        server.stop();

        ClientException exception = assertThrows(ClientException.class, () -> client.getBook(1L));

        assertInstanceOf(ResourceAccessException.class, exception.getCause());
    }

    @Test
    void wrapsReadTimeouts() {
        server.respond("/api/books/1", 200, SINGLE_BOOK_RESPONSE, Duration.ofSeconds(6));

        ClientException exception = assertThrows(ClientException.class, () -> client.getBook(1L));

        assertInstanceOf(ResourceAccessException.class, exception.getCause());
    }

    private static final class StubServer {

        private final HttpServer httpServer;
        private final ExecutorService executor = Executors.newCachedThreadPool();
        private final Map<String, StubResponse> responses = new ConcurrentHashMap<>();
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

        private void respond(String path, int status, String body) {
            respond(path, status, body, Duration.ZERO);
        }

        private void respond(String path, int status, String body, Duration delay) {
            responses.put(path, new StubResponse(status, body, delay));
        }

        private void handleRequest(HttpExchange exchange) throws IOException {
            try {
                StubResponse response = responses.getOrDefault(
                        exchange.getRequestURI().getPath(),
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
