package com.siemens.internship.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.siemens.internship.model.Item;
import com.siemens.internship.repository.ItemRepository;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
public class ItemControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ItemRepository itemRepository;

    private static final String FIXTURE_PATH = "src/test/resources/fixtures/";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws Exception {
        itemRepository.deleteAll();
        itemRepository.flush();
        seedDatabase();
    }

    private void seedDatabase() throws Exception {
        String seedDataJSON = loadFixture("item_seed.json");
        List<Item> users = objectMapper.readValue(seedDataJSON, new TypeReference<List<Item>>() {});
        itemRepository.saveAll(users);
    }

    private String loadFixture(String fileName) throws IOException {
        return Files.readString(Paths.get(FIXTURE_PATH + fileName));
    }

    @Test
    void testGetAllItems() throws Exception {
        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[*].name", Matchers.containsInAnyOrder("p1", "p2", "p3", "p4", "p5")))
                .andExpect(jsonPath("$[*].status", Matchers.containsInAnyOrder("ADDED", "ADDED", "ADDED", "ADDED", "UPDATED")));

    }

    @Test
    void testAddNewValidItem() throws Exception {
        String validItemJson = loadFixture("valid_item.json");

        mockMvc.perform(post("/api/items")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validItemJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("pNew"))
                .andExpect(jsonPath("$.email").value("new@mail.com"));

    }

    @Test
    void testAddNewInvalidItem() throws Exception {
        String invalidItemJson = loadFixture("invalid_item.json");

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidItemJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("Email does not have the expected format."));

    }

    @Test
    void testUpdateExistingItem() throws Exception {
        String updateJson = loadFixture("valid_update_item.json");

        mockMvc.perform(put("/api/items/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("pNew"))
                .andExpect(jsonPath("$.status").value("UPDATED"));
    }

    @Test
    void testUpdateInvalidItem() throws Exception {
        String invalidUpdate = loadFixture("invalid_item.json");

        mockMvc.perform(put("/api/items/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidUpdate))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").value("Email does not have the expected format."));
    }

    @Test
    void testUpdateNonExistingItem() throws Exception {
        String body = loadFixture("valid_update_item.json");

        mockMvc.perform(put("/api/items/{id}", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("There is no item with the id 999"));
    }

    @Test
    void testDeleteExistingItem() throws Exception {
        mockMvc.perform(delete("/api/items/{id}", 1))
                .andExpect(status().isNoContent());
    }

    @Test
    void testProcessItems() throws Exception {
        MvcResult mvcResult = mockMvc.perform(get("/api/items/process"))
                .andReturn();

        assertTrue(mvcResult.getRequest().isAsyncStarted());

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[*].status", everyItem(equalTo("PROCESSED"))));

        List<Item> all = itemRepository.findAll();
        for (Item item : all) {
            assertEquals("PROCESSED", item.getStatus());
        }
    }
}
