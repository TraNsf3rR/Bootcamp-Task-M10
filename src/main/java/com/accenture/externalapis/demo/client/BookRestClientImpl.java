package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BookRestClientImpl implements BookRestClient {

    private final RestClient restClient;

    public BookRestClientImpl(RestClient.Builder builder, ExternalServiceProperties properties) {
        this.restClient = builder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public BookDto getBook(Long id) {
        BookApiResponse response = restClient.get()
                .uri("/books/{id}", id)
                .retrieve()
                .body(BookApiResponse.class);
        return response.toBookDto();
    }

    @Override
    public List<BookDto> getAllBooks() {
        BookApiResponse[] responses = restClient.get()
                .uri("/books")
                .retrieve()
                .body(BookApiResponse[].class);
        return Arrays.stream(responses)
                .map(BookApiResponse::toBookDto)
                .toList();
    }
}
