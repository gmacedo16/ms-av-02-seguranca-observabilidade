package br.edu.espm.msav02.api;

import java.util.Set;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class ApiModels {
    private ApiModels() {
    }

    public record LoginRequest(@NotBlank @Size(max = 100) String username, @NotBlank String senha) {
    }

    public record CreateUserRequest(@NotBlank @Size(max = 100) String username,
                                    @NotBlank @Size(min = 8, max = 100) String senha) {
    }

    public record ChangePasswordRequest(@NotBlank String atual,
                                        @NotBlank @Size(min = 8, max = 100) String nova) {
    }

    public record LoginResponse(String username) {
    }

    public record UserResponse(String username, Set<String> roles, boolean enabled, boolean locked) {
    }
}
