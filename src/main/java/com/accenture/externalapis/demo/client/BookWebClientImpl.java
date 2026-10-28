package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import com.accenture.externalapis.demo.dto.BookApiResponse;
import com.accenture.externalapis.demo.dto.BookDto;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class BookWebClientImpl implements BookWebClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final WebClient webClient;

    public BookWebClientImpl(WebClient.Builder builder, ExternalServiceProperties properties) {
        this.webClient = builder.baseUrl(properties.baseUrl()).build();
    }

    @Override
    public Mono<BookDto> getBookAsync(Long id) {
        String endpoint = "GET /books/" + id;
        Mono<BookApiResponse> response = webClient.get()
                .uri("/books/{id}", id)
                .retrieve()
                .onStatus(HttpStatusCode::isError, error -> mapResponseError(error, endpoint))
                .bodyToMono(BookApiResponse.class)
                .switchIfEmpty(Mono.error(
                        new ClientException("External service returned an empty response for " + endpoint)))
                .timeout(REQUEST_TIMEOUT);
        return mapRequestErrors(response, endpoint)
                .map(book -> toBookDto(book, endpoint));
    }

    @Override
    public Flux<BookDto> getAllBooksAsync() {
        String endpoint = "GET /books";
        Mono<BookApiResponse[]> response = webClient.get()
                .uri("/books")
                .retrieve()
                .onStatus(HttpStatusCode::isError, error -> mapResponseError(error, endpoint))
                .bodyToMono(BookApiResponse[].class)
                .switchIfEmpty(Mono.error(
                        new ClientException("External service returned an empty response for " + endpoint)))
                .timeout(REQUEST_TIMEOUT);
        return mapRequestErrors(response, endpoint)
                .flatMapMany(Flux::fromArray)
                .map(book -> toBookDto(book, endpoint));
    }

    @Override
    public Mono<List<BookDto>> getBooksInParallel(Long id1, Long id2) {
        return Mono.zip(getBookAsync(id1), getBookAsync(id2))
                .map(tuple -> List.of(tuple.getT1(), tuple.getT2()));
    }

    private Mono<? extends Throwable> mapResponseError(ClientResponse response, String endpoint) {
        return response.createException()
                .map(exception -> new ClientException(
                        "External service returned HTTP " + exception.getStatusCode().value()
                                + " for " + endpoint,
                        exception));
    }

    private <T> Mono<T> mapRequestErrors(Mono<T> response, String endpoint) {
        return response
                .onErrorMap(WebClientRequestException.class,
                        exception -> new ClientException(
                                "Could not access the external service for " + endpoint,
                                exception))
                .onErrorMap(TimeoutException.class,
                        exception -> new ClientException(
                                "External service request timed out for " + endpoint,
                                exception))
                .onErrorMap(DecodingException.class,
                        exception -> new ClientException(
                                "Could not decode the external service response for " + endpoint,
                                exception))
                .onErrorMap(WebClientException.class,
                        exception -> new ClientException(
                                "External service request failed for " + endpoint,
                                exception));
    }

    private BookDto toBookDto(BookApiResponse response, String endpoint) {
        if (response == null) {
            throw new ClientException("External service returned a null book for " + endpoint);
        }
        return response.toBookDto();
    }
}
