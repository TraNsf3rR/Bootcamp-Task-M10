package com.accenture.externalapis.demo.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookDto;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
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

    private static final class StubServer {

        private final HttpServer httpServer;
        private final ExecutorService executor = Executors.newCachedThreadPool();
        private final Map<String, String> responses = new ConcurrentHashMap<>();
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
            responses.put(path, body);
        }

        private Set<String> requestedPaths() {
            return Set.copyOf(requestedPaths);
        }

        private void handleRequest(HttpExchange exchange) throws IOException {
            try {
                String path = exchange.getRequestURI().getPath();
                requestedPaths.add(path);
                boolean found = responses.containsKey(path);
                byte[] body = responses.getOrDefault(path, "not found").getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(found ? 200 : 404, body.length);
                try (OutputStream responseBody = exchange.getResponseBody()) {
                    responseBody.write(body);
                }
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
}
