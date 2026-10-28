package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.config.ExternalServiceProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

// TODO: Make this class implement BookRestClient.
@Component
public class BookRestClientImpl {

    private RestClient restClient;

    public BookRestClientImpl(RestClient.Builder builder, ExternalServiceProperties properties) {
        // TODO: Build the RestClient using builder.baseUrl(properties.baseUrl()).build()
        // and assign it to this.restClient
        //
        // Optional/bonus: this service doesn't require auth, but in a real API you would
        // often also add builder.defaultHeader("Authorization", "Bearer " + token) here.
    }

    // TODO: Implement getBook(Long id) - fetch one book from GET /books/{id} as a
    // BookApiResponse, then map it onto a BookDto (only keep the fields BookDto needs).
    //
    // TODO: Handle the main RestClient error cases and rethrow them as ClientException:
    //  - HttpClientErrorException (4xx, e.g. book not found)
    //  - HttpServerErrorException (5xx, e.g. the faulty/teapot book)
    //  - ResourceAccessException (connection refused / timeout - the external service is unreachable)

    // TODO: Implement getAllBooks() - fetch all books from GET /books as
    // BookApiResponse[], then map each one onto a BookDto. Handle the same error
    // cases as getBook() above.
}
