package com.accenture.externalapis.demo.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class BookApiResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesExternalBookResponse() throws Exception {
        String json = """
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

        BookApiResponse response = objectMapper.readValue(json, BookApiResponse.class);

        assertEquals(1L, response.id());
        assertEquals("The Spaghetti Whisperer", response.title());
        assertEquals("Sir Reginald Fluffington III", response.author());
        assertEquals("Software Engineering", response.genre());
        assertEquals(34.99, response.price());
        assertEquals("978-0132350884", response.isbn());
        assertEquals(2008, response.publishedYear());
    }
}
