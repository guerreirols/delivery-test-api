package com.deliverytest.api.order.controller;

import com.deliverytest.api.order.dto.OrderRequest;
import com.deliverytest.api.order.dto.OrderResponse;
import com.deliverytest.api.order.dto.OrderStatusUpdateRequest;
import com.deliverytest.api.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(
        name = "Pedidos",
        description = "Endpoints para gerenciamento de pedidos"
)
@RestController
@RequestMapping("/orders")
@AllArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @Operation(
            summary = "Criar pedido",
            description = "Cria um novo pedido de entrega com cliente, itens e endereço de entrega."
    )
    @PostMapping()
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest orderRequest) {
        return ResponseEntity.ok(orderService.create(orderRequest));
    }

    @Operation(
            summary = "Atualizar status do pedido",
            description = "Atualiza o status de um pedido existente."
    )
    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String id,
                                          @Valid @RequestBody OrderStatusUpdateRequest request) {
        orderService.updateStatus(id, request);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Buscar pedido por ID",
            description = "Retorna os detalhes de um pedido específico pelo seu ID."
    )
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> findById(@PathVariable String id) {
        return ResponseEntity.ok(orderService.findById(id));
    }

    @Operation(
            summary = "Buscar todos os pedidos",
            description = "Retorna uma lista paginada de todos os pedidos, ordenados do mais recente para o mais antigo.",
            parameters = {
                    @Parameter(name = "page", in = ParameterIn.QUERY, description = "Número da página (começa em 0)",
                            schema = @Schema(type = "integer", defaultValue = "0")),
                    @Parameter(name = "size", in = ParameterIn.QUERY, description = "Itens por página",
                            schema = @Schema(type = "integer", defaultValue = "10"))
            }
    )
    @GetMapping
    public ResponseEntity<Page<OrderResponse>> findAll(
            @Parameter(hidden = true)
            @PageableDefault(size = 10) Pageable pageable) {

        Pageable sorted = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        return ResponseEntity.ok(orderService.findAll(sorted));
    }
}
