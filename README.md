## Siemens Java Internship - Code Refactoring Project

In this README I will present in more detail the changes made to the previous code and why I decided to refactor the code the way I did. 
The code also includes some comments, but this README is more detailed.

## New files added
In the previous service class, the @Async was used, but needed to be enabled with @EnableAsync, otherwise @Async is ignored.
In the package config, I created the class AsyncConfig that enables the async annotation and ,by being defined as a bean. @Asyncpicks the executor and borrows a thread each time it is called. In that class, I also define the pool of threads that is going to be used by the executor with the asyncronous methods that use this bean.

I have also created a custom exception "IdNotExistentException" that I used when the find by id returned an empty optional. In order to define the stutus codes for the exceptions, I created a GlobalExceptionHandler class that assigns a NOT EXISTENT status code for the IdNotExistentException and a BAD REQUEST status code when the email associated with the item does not follow the required format.

## Email validation
In the Item model class, I used the annotation @Email and a regex in order to define the valid structure of an email address.
For my email address, before '@' there can be one or more letter (lower or uppercase), numbers and . - _. Then after the '@' there are one or more letters or numbers followed by a '.' and 2 or 3 letters. 

## Refactoring the code
I made changes in the service and controller class such that the service uses a functional programming style, communicates with the database, but not before checking if the ids (if it is the case) actually exist in the database. Controller only takes the answer from the service and decides which status code is returned. 
The status codes that i used are:
 * HttpStatus.OK - the action was successful and an item / a list of items was returned
 * HttpStatus.CREATED - the action was successful and a new item was created (used also for update because PUT creates a whole new entry instead of the alreadyexisting one, even if only one value was changed)
 * HttpStatus.INTERNAL_SERVER_ERROR - the action was not successful because of some server error (e.g. the connection to the database failed)
 * HttpStatus.NO_CONTENT - the action was successful, but no content was returned - used for delete
 * HttpStatus.BAD_REQUEST (defined in the GlobalExceptionHandler) - the action was not successful because the email does not have the specified format
 * HttpStatus.NOT_FOUND - the action was not successful because the user tried to update or delete an item giving a non-existent id

## Asynchronous processing of the data
#### 1. Synchronous return
- **Problem:** `processItemsAsync` returned `List<Item>`, so despite the `@Async` annotation Spring executed synchronously and responded before work finished.  
- **Solution:** Changed the method signature to `CompletableFuture<List<Item>>`, enabling true non-blocking behaviour and future chaining.

#### 2. Unsafe shared state
- **Problem:** A plain `ArrayList` and an `int` counter were accessed by multiple threads, leading to race conditions and data leakage between requests.  
- **Solution:** Replaced them with `Collections.synchronizedList(new ArrayList<>())` and `AtomicInteger`, both declared inside the method to keep each invocation isolated.

#### 3. Lost futures
- **Problem:** `CompletableFuture.runAsync(...)` was used but its handle was discarded; there was no `join()`/`allOf()` so the method never waited for completion.  
- **Solution:** Collected every child future into `CompletableFuture.allOf(...)`; the outer future completes only after all tasks finish.

#### 4. Ad-hoc thread pool
- **Problem:** The code created its own `Executors.newFixedThreadPool(10)`, bypassing Spring’s management and metrics.  
- **Solution:** Introduced `AsyncConfig` and referenced that bean via `@Async("itemProcessingExecutor")`, integrating with Spring’s lifecycle and configuration.

#### 5. Artificial delay
- **Problem:** `Thread.sleep(100)` throttled throughput and wasted worker threads.  
- **Solution:** Removed the unnecessary delay entirely.

#### 6. Silent errors
- **Problem:** Only `InterruptedException` was caught; any other exception vanished into the pool’s default handler, so the caller never learned of failures.  
- **Solution:** Let `CompletableFuture` propagate exceptions; unhandled errors bubble up and are formatted by `GlobalExceptionHandler` while still being logged.


## Tests
### Unit tests on the service class
They run without the Spring container; repositories are mocked with Mockito and a real `ThreadPoolTaskExecutor` from `AsyncConfig` is supplied.

### Integration tests on the controller class
These tests start the full Spring context and hit the REST endpoints through MockMvc, using an in-memory H2 database configured via `application-test.properties`.

For both type of tests, I tested all scenarios I thought about: get all items, process them, add a new item (successfully and not), update a new item (successfully and not), delete an item.
