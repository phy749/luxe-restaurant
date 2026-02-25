package com.luxe_restaurant.app.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luxe_restaurant.app.requests.dish.DishRequest;
import com.luxe_restaurant.app.responses.dish.DishResponse;
import com.luxe_restaurant.domain.services.DishService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DishController.class)
class DishControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DishService dishService;

    @Autowired
    private ObjectMapper objectMapper;

    private DishRequest dishRequest;
    private DishResponse dishResponse;

    @BeforeEach
    void setUp() {
        dishRequest = DishRequest.builder()
                .dishName("Pho Bo")
                .price(new BigDecimal("65000"))
                .categoryId(1L)
                .des("Traditional Vietnamese beef noodle soup")
                .urlImage("https://example.com/pho.jpg")
                .build();

        dishResponse = new DishResponse();
        dishResponse.setId(1L);
        dishResponse.setName("Pho Bo");
        dishResponse.setPrice(new BigDecimal("65000"));
        dishResponse.setCategoryId(1L);
        dishResponse.setCategoryName("Main Course");
        dishResponse.setDes("Traditional Vietnamese beef noodle soup");
        dishResponse.setUrlImage("https://example.com/pho.jpg");
        dishResponse.setActive(true);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createDish_ShouldReturnCreatedDish_WhenValidRequest() throws Exception {
        when(dishService.createDish(any(DishRequest.class))).thenReturn(dishResponse);

        mockMvc.perform(post("/api/dish/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dishRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Pho Bo"))
                .andExpect(jsonPath("$.price").value(65000))
                .andExpect(jsonPath("$.active").value(true));

        verify(dishService).createDish(any(DishRequest.class));
    }

    @Test
    @WithMockUser(roles = "USER")
    void createDish_ShouldReturnForbidden_WhenNotAdmin() throws Exception {
        mockMvc.perform(post("/api/dish/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dishRequest)))
                .andExpect(status().isForbidden());

        verify(dishService, never()).createDish(any(DishRequest.class));
    }

    @Test
    void createDish_ShouldReturnBadRequest_WhenInvalidData() throws Exception {
        DishRequest invalidRequest = DishRequest.builder()
                .dishName("") // Invalid: empty name
                .price(new BigDecimal("-10")) // Invalid: negative price
                .build();

        mockMvc.perform(post("/api/dish/create")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllDishes_ShouldReturnDishList() throws Exception {
        List<DishResponse> dishes = Arrays.asList(dishResponse);
        when(dishService.getAllDishes()).thenReturn(dishes);

        mockMvc.perform(get("/api/dish/getall"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpected(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].name").value("Pho Bo"));

        verify(dishService).getAllDishes();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateDish_ShouldReturnUpdatedDish_WhenValidRequest() throws Exception {
        when(dishService.updateDish(eq(1L), any(DishRequest.class))).thenReturn(dishResponse);

        mockMvc.perform(put("/api/dish/update/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dishRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Pho Bo"));

        verify(dishService).updateDish(eq(1L), any(DishRequest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteDish_ShouldReturnNoContent_WhenDishExists() throws Exception {
        doNothing().when(dishService).deleteDish(1L);

        mockMvc.perform(delete("/api/dish/delete/1")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(dishService).deleteDish(1L);
    }

    @Test
    void findDish_ShouldReturnDish_WhenDishExists() throws Exception {
        when(dishService.getDishById(1L)).thenReturn(dishResponse);

        mockMvc.perform(get("/api/dish/find/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.name").value("Pho Bo"));

        verify(dishService).getDishById(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void toggleDish_ShouldReturnUpdatedDish() throws Exception {
        dishResponse.setActive(false);
        when(dishService.toggleDishStatus(1L)).thenReturn(dishResponse);

        mockMvc.perform(put("/api/dish/toggle/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.active").value(false));

        verify(dishService).toggleDishStatus(1L);
    }
}