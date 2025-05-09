package com.siemens.internship.controller;

import com.siemens.internship.config.exception.IdNotExistentException;
import com.siemens.internship.service.ItemService;
import com.siemens.internship.model.Item;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * I modified the Http status codes in order to show correctly if the action was successful or nor.
 * HttpStatus.OK - the action was successful and an item / a list of items was returned
 * HttpStatus.CREATED - the action was successful and a new item was created (used also for update because PUT creates a whole new entry instead of the already
 *                      existing one, even if only one value was changed)
 * HttpStatus.INTERNAL_SERVER_ERROR - the action was not successful because of some server error (e.g. the connection to the database failed)
 * HttpStatus.NO_CONTENT - the action was successful, but no content was returned - used for delete
 * HttpStatus.BAD_REQUEST (defined in the GlobalExceptionHandler) - the action was not successful because the email does not have the specified format
 * HttpStatus.NOT_FOUND - the action was not successful because the user tried to update or delete an item giving a non-existent id
 */

@RestController
@AllArgsConstructor
@RequestMapping("/api/items")
public class ItemController {
    private ItemService itemService;

    @GetMapping
    public ResponseEntity<List<Item>> getAllItems() {
        return ResponseEntity.ok(itemService.getAllItems());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Item> getItemById(@PathVariable Long id) throws IdNotExistentException {
        Item item = itemService.findById(id);

        return ResponseEntity.ok(item);
    }

    @PostMapping
    public ResponseEntity<Item> createItem(@Valid @RequestBody Item item) {
        return itemService.createItem(item)
                .map(saved -> ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(saved))
                .orElseGet(
                        () -> ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(null)
                );
    }

    @PutMapping("/{id}")
    public ResponseEntity<Item> updateItem(@PathVariable Long id, @Valid @RequestBody Item newItem) throws IdNotExistentException {
        Item updated = itemService.updateItem(id, newItem);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(updated);
    }



    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        itemService.deleteById(id);

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(null);
    }

    /**
     * The return type for this method was changed accordingly to the service method for processing
     *
     */

    @GetMapping("/process")
    public CompletableFuture<ResponseEntity<List<Item>>> processItems() {
        return itemService
                .processItemsAsync()
                .thenApply(ResponseEntity::ok);
    }
}
