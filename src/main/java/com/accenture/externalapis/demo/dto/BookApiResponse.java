package com.accenture.externalapis.demo.dto;

public record BookApiResponse(
        Long id,
        String title,
        String author,
        String genre,
        Double price,
        String isbn,
        Integer publishedYear) {
}
