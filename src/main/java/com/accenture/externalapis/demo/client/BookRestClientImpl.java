package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class BookRestClientImpl implements BookRestClient {

    private static final int CONNECT_TIMEOUT_MILLIS = 5_000;
    private static final int READ_TIMEOUT_MILLIS = 5_000;

    private final RestClient restClient;

    public BookRestClientImpl(RestClient.Builder builder, ExternalServiceProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        requestFactory.setReadTimeout(READ_TIMEOUT_MILLIS);
        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public BookDto getBook(Long id) {
        String endpoint = "GET /books/" + id;
        return execute(endpoint, () -> {
            BookApiResponse response = restClient.get()
                    .uri("/books/{id}", id)
                    .retrieve()
                    .body(BookApiResponse.class);
            if (response == null) {
                throw new ClientException("External service returned an empty response for " + endpoint);
            }
            return response.toBookDto();
        });
    }

    @Override
    public List<BookDto> getAllBooks() {
        String endpoint = "GET /books";
        return execute(endpoint, () -> {
            BookApiResponse[] responses = restClient.get()
                    .uri("/books")
                    .retrieve()
                    .body(BookApiResponse[].class);
            if (responses == null) {
                throw new ClientException("External service returned an empty response for " + endpoint);
            }
            return Arrays.stream(responses)
                    .map(response -> {
                        if (response == null) {
                            throw new ClientException("External service returned a null book for " + endpoint);
                        }
                        return response.toBookDto();
                    })
                    .toList();
        });
    }

    private <T> T execute(String endpoint, Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException ex) {
            throw new ClientException(
                    "External service returned HTTP " + ex.getStatusCode().value() + " for " + endpoint,
                    ex);
        } catch (ResourceAccessException ex) {
            throw new ClientException("Could not access the external service for " + endpoint, ex);
        } catch (RestClientException ex) {
            throw new ClientException("Could not process the external service response for " + endpoint, ex);
        }
    }
}
