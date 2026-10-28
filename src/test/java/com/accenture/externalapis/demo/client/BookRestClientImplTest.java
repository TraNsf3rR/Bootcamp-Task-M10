package com.accenture.externalapis.demo.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookDto;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class BookRestClientImplTest {

    private static final String BASE_URL = "https://books.example.test/api";

    @Test
    void getBookFetchesAndMapsSingleBook() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        BookRestClient client = new BookRestClientImpl(
                builder,
                new ExternalServiceProperties(BASE_URL));
        String response = """
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

        server.expect(requestTo(BASE_URL + "/books/1"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        assertEquals(new BookDto(
                "The Spaghetti Whisperer",
                "Sir Reginald Fluffington III",
                "Software Engineering",
                34.99), client.getBook(1L));

        server.verify();
    }

    @Test
    void getAllBooksFetchesAndMapsEveryBook() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        BookRestClient client = new BookRestClientImpl(
                builder,
                new ExternalServiceProperties(BASE_URL));
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

        server.expect(requestTo(BASE_URL + "/books"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        assertEquals(List.of(
                new BookDto(
                        "The Spaghetti Whisperer",
                        "Sir Reginald Fluffington III",
                        "Software Engineering",
                        34.99),
                new BookDto("Reactive Recipes", "Ada Example", "Technology", 19.5)),
                client.getAllBooks());

        server.verify();
    }
}
