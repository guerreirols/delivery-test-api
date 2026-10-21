package com.deliverytest.api.order.controller;

import com.deliverytest.api.exception.GlobalExceptionHandler;
import com.deliverytest.api.order.dto.OrderRequest;
import com.deliverytest.api.order.dto.OrderResponse;
import com.deliverytest.api.order.dto.OrderStatusUpdateRequest;
import com.deliverytest.api.order.entity.OrderStatus;
import com.deliverytest.api.order.exception.OrderNotFoundException;
import com.deliverytest.api.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class OrderControllerTest {
    private final OrderService service = mock(OrderService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("messages");
        messages.setDefaultEncoding("UTF-8");
        mvc = MockMvcBuilders.standaloneSetup(new OrderController(service))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler(messages)).build();
    }

    @Test
    void unexpectedFailureReturnsSafe500() throws Exception {
        when(service.findById("order-id")).thenThrow(new IllegalStateException("password=secret"));

        mvc.perform(get("/orders/order-id")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Não foi possível concluir a operação. Tente novamente mais tarde"));
    }

    @Test
    void unsupportedHttpMethodRetains405InsteadOfBecoming500() throws Exception {
        mvc.perform(delete("/orders")).andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(service);
    }

    @Test
    void unsupportedMediaTypeRetains415InsteadOfBecoming500() throws Exception {
        mvc.perform(post("/orders").contentType(MediaType.TEXT_PLAIN).content("invalid"))
                .andExpect(status().isUnsupportedMediaType());
        verifyNoInteractions(service);
    }

    @Test
    void createsOrderUsingExistingHttpContract() throws Exception {
        when(service.create(any(OrderRequest.class))).thenReturn(OrderResponse.builder()
                .id("order-id").customerName("Maria").status(OrderStatus.RECEBIDO).build());

        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content("""
                {"customerName":"Maria","deliveryAddress":"Rua A",
                 "items":[{"name":"Pizza","quantity":1,"unitPrice":29.90}]}
                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value("order-id"))
                .andExpect(jsonPath("$.status").value("RECEBIDO"));
    }

    @Test
    void invalidNestedItemsDoNotReachService() throws Exception {
        mvc.perform(post("/orders").contentType(MediaType.APPLICATION_JSON).content("""
                {"customerName":"Maria","deliveryAddress":"Rua A",
                 "items":[{"name":"Pizza","quantity":0,"unitPrice":-1}]}
                """))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors").isArray());
        verifyNoInteractions(service);
    }

    @Test
    void missingOrderReturnsStandardError() throws Exception {
        when(service.findById("missing")).thenThrow(new OrderNotFoundException("missing"));

        mvc.perform(get("/orders/missing")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updateStatusPassesIdentifierAndRequestedStatus() throws Exception {
        mvc.perform(put("/orders/order-id/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"EM_PREPARO\"}"))
                .andExpect(status().isOk()).andExpect(content().string(""));

        verify(service).updateStatus("order-id", new OrderStatusUpdateRequest(OrderStatus.EM_PREPARO));
    }

    @Test
    void nullStatusDoesNotReachService() throws Exception {
        mvc.perform(put("/orders/order-id/status").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    @Test
    void paginationAlwaysUsesNewestFirstWithoutChangingPageAndSize() throws Exception {
        when(service.findAll(any(Pageable.class))).thenReturn(Page.empty(PageRequest.of(2, 5)));

        mvc.perform(get("/orders").param("page", "2").param("size", "5").param("sort", "customerName,asc"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> page = ArgumentCaptor.forClass(Pageable.class);
        verify(service).findAll(page.capture());
        assertThat(page.getValue().getPageNumber()).isEqualTo(2);
        assertThat(page.getValue().getPageSize()).isEqualTo(5);
        assertThat(page.getValue().getSort()).isEqualTo(Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
