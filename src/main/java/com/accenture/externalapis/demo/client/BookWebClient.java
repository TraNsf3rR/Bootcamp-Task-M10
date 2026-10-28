package com.accenture.externalapis.demo.client;

import com.accenture.externalapis.demo.dto.BookDto;
import java.util.List;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

// This is an interface - implement it in BookWebClientImpl using WebClient
// (reactive/async calls) to talk to the external book service.
public interface BookWebClient {

    Mono<BookDto> getBookAsync(Long id);

    Flux<BookDto> getAllBooksAsync();

    Mono<List<BookDto>> getBooksInParallel(Long id1, Long id2);
}
