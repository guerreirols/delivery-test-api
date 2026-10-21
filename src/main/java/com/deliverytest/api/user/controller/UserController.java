package com.deliverytest.api.user.controller;

import com.deliverytest.api.user.dto.UserProfileResponse;
import com.deliverytest.api.user.service.UserProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Usuários", description = "Dados do usuário autenticado")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserProfileService profiles;

    @Operation(summary = "Consultar meu perfil", description = "Retorna o nome do usuário autenticado.")
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> current(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(profiles.findCurrent(jwt.getSubject()));
    }
}
