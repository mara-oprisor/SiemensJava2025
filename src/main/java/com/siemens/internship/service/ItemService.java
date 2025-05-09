package com.siemens.internship.service;

import com.siemens.internship.config.exception.IdNotExistentException;
import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import lombok.AllArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * PROBLEMS WITH THE INITIAL IMPLEMENTATION:
 *      1. processItemsAsync cannot have as return type List<Item> because even if the method is marked as @Async Spring will return
 *         immediately even if the result is not ready (it is returned synchronously). Therefore, the return type is changed into CompletableFuture<List<Item>>
 *         such that it allows us to write asynchronous, non-blocking code and which allows us to chain more operations.
 *      2. processedArray was a plain array list and processedCount is an int. Both of them are shared by the whole pool of threads.
 *         Therefore, race conditions and lost updates might appear. I will change them into Collections.synchronizedList,
 *         respectively AtomicInteger such that they handle synchronisation.
 *         Moreover, by keeping them as attribute of the instance they will grow across more calls. Therefore, we will move them in the method body.
 *      3. CompletableFuture.runAsync()is used without saving or combining those futures. There’s no join() or allOf() to wait for “all” tasks to finish.
 *      4. Executors.newFixedThreadPool(10) is replaced with a Spring supported Executor(it is defined in the AsyncConfig file)
 *         (method is marked as @Async and should have been handled by the Spring, but we used owr own executor).
 *      5. Thread.sleep(100) reduces throughput.
 *      6. Errors are not reported accordingly, it catches only InterruptedException.
 *         Any other exception escapes the lambda, gets handled by the thread‐pool’s default handler, and the caller never learns of it.
 */

@Service
@AllArgsConstructor
public class ItemService {
    private ItemRepository itemRepository;
    private Executor executor;

    public List<Item> getAllItems() {
        return itemRepository.findAll();
    }

    public Item findById(Long id) throws IdNotExistentException {
        return itemRepository.findById(id)
                .orElseThrow(
                        () -> new IdNotExistentException("There is no item with the id " + id)
                );
    }

    public Optional<Item> createItem(Item item) {
        return Stream.of(item)
                .map(itemRepository :: save)
                .findFirst();
    }

    public Item updateItem(Long id, Item newItem) throws IdNotExistentException {
        return itemRepository.findById(id)
                .map(item -> {
                        item.setId(id);
                        item.setName(newItem.getName());
                        item.setDescription(newItem.getDescription());
                        item.setStatus(newItem.getStatus());
                        item.setEmail(newItem.getEmail());

                        return item;
                })
                .map(itemRepository :: save)
                .orElseThrow(
                        () -> new IdNotExistentException("There is no item with the id " + id)
                );
    }


    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }


    /**
     * Method grabs the itemProcessingExecutor bean that was defined in the AsyncConfig configuration file.
     *
     * As mentioned before, processedItems and processedCount are declared in order to be thread-safe.
     *
     * I used a functional programming approach in order to promote immutability and the asynchronous approach.
     * All ids are fetched and for each of them I get the corresponding item and mark it as processed.
     * allOf() returns a CompletableFuture that completes when all the items passed the processing
     */
    @Async("itemProcessingExecutor")
    public CompletableFuture<List<Item>> processItemsAsync() {
        List<Item> processedItems = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger processedCount = new AtomicInteger(0);

        return CompletableFuture
                .allOf(
                        itemRepository.findAllIds().stream()
                                .map(id ->
                                        CompletableFuture
                                                .supplyAsync(() -> itemRepository.findById(id).orElse(null), executor)
                                                .thenAccept(item -> {
                                                    if (item == null) return;
                                                    item.setStatus("PROCESSED");
                                                    itemRepository.save(item);
                                                    processedItems.add(item);
                                                    processedCount.incrementAndGet();
                                                })
                                )
                                .toArray(CompletableFuture[]::new)
                )
                .thenApply(v -> processedItems);
    }
}

