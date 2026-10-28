package com.accenture.externalapis.demo.dto;

// This is the domain DTO used by this application.
// Compare it to BookApiResponse (the raw fields returned by the external
// service, which you define yourself using Swagger UI) - notice this DTO
// does not keep every field. Decide what you truly need.
public record BookDto(String title,
                       String author,
                       String genre,
                       double price) {
}