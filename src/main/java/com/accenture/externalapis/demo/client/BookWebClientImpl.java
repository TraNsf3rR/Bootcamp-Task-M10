package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

// TODO: Make this class implement BookWebClient.
@Component
public class BookWebClientImpl {

    private WebClient webClient;

    public BookWebClientImpl(WebClient.Builder builder, ExternalServiceProperties properties) {
        // TODO: Build the WebClient using builder.baseUrl(properties.baseUrl()).build()
        // and assign it to this.webClient
        //
        // Optional/bonus: this service doesn't require auth, but in a real API you would
        // often also add builder.defaultHeader("Authorization", "Bearer " + token) here.
    }

    // TODO: Implement getBookAsync(Long id) - fetch one book from GET /books/{id} as
    // Mono<BookApiResponse>, then map it onto a Mono<BookDto>.
    //
    // TODO: Handle the main WebClient error cases and rethrow them as ClientException,
    // e.g. via onStatus()/onErrorResume():
    //  - WebClientResponseException (4xx/5xx, e.g. book not found or the faulty/teapot book)
    //  - WebClientRequestException (connection refused / timeout - the external service is unreachable)

    // TODO: Implement getAllBooksAsync() - fetch all books from GET /books as
    // Flux<BookApiResponse>, then map each one onto a BookDto. Handle the same error
    // cases as getBookAsync() above.

    // TODO: Implement getBooksInParallel(Long id1, Long id2) - fetch two books in
    // parallel with Mono.zip(). Handle the same error cases as getBookAsync() above.
}
