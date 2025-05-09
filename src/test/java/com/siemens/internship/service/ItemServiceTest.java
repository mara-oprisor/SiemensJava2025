package com.siemens.internship.service;

import com.siemens.internship.config.AsyncConfig;
import com.siemens.internship.config.exception.IdNotExistentException;
import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * I am performing UNIT TESTS on the service class for items
 * Each test is going to follow the pattern GIVEN-WHEN-THEN
 */

public class ItemServiceTest {
    @Mock
    private ItemRepository itemRepository;

    private Executor executor;
    private ItemService itemService;


    private List<Item> items;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        executor = new AsyncConfig().itemProcessingExecutor();
        itemService = new ItemService(itemRepository, executor);
        items = List.of(new Item(1L, "p1", "desc", "ADDED", "u@mail.com"),
                        new Item(2L, "p2", "desc1", "ADDED", "u2@mail.com"));
    }

    @Test
    void testGetAllItems() {
        // given - the items from setUp()

        //when
        when(itemRepository.findAll()).thenReturn(items);
        List<Item> result = itemService.getAllItems();

        //then
        assertEquals(items, result);
        verify(itemRepository, times(1)).findAll();
    }

    @Test
    void testAddValidClient() {
        // given
        Item newItem = new Item(null, "newP", "newD", "ADDED", "newEmail@mail.com");
        Item savedItem = new Item(3L, "newP", "newD", "ADDED", "newEmail@mail.com");
        //when
        when(itemRepository.save(newItem)).thenReturn(savedItem);
        Item result = itemService.createItem(newItem).get();

        //then
        assertEquals(savedItem, result);
        assertNotNull(result.getId());
        verify(itemRepository, times(1)).save(any());
    }

    @Test
    void testUpdateExistingItem() throws IdNotExistentException {
        //given
        Long id = 1L;
        Item existing = items.get(0);
        Item newItem = new Item(null, "newP", "newD", "UPDATED", "newEmail@mail.com");

        //when
        when(itemRepository.findById(id)).thenReturn(Optional.of(existing));
        when(itemRepository.save(existing)).thenReturn(existing);
        Item result = itemService.updateItem(id, newItem);

        //then
        assertEquals(id, result.getId());
        assertEquals("newP", result.getName());
        assertEquals("newD", result.getDescription());
        assertEquals("UPDATED", result.getStatus());
        assertEquals("newEmail@mail.com", result.getEmail());
        verify(itemRepository, times(1)).findById(id);
        verify(itemRepository, times(1)).save(existing);
    }

    @Test
    void testDeleteItem() {
        //given
        Long id = 1L;
        doNothing().when(itemRepository).deleteById(id);

        // when
        itemService.deleteById(id);

        // then
        verify(itemRepository, times(1)).deleteById(id);
    }

    @Test
    void testProcessItems() throws ExecutionException, InterruptedException {
        // given
        List<Long> ids = List.of(1L, 2L);

        //when
        when(itemRepository.findAllIds()).thenReturn(ids);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(items.get(0)));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(items.get(1)));
        when(itemRepository.save(any(Item.class))).thenAnswer(inv -> inv.getArgument(0));
        List<Item> result = itemService.processItemsAsync().get();

        // then
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(i -> "PROCESSED".equals(i.getStatus())));
        verify(itemRepository).findAllIds();
        verify(itemRepository, times(2)).findById(anyLong());
        verify(itemRepository, times(2)).save(any(Item.class));
    }
}
