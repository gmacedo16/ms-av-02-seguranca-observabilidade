package br.edu.espm.msav02.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import br.edu.espm.msav02.api.ApiModels.ChangePasswordRequest;
import br.edu.espm.msav02.api.ApiModels.CreateUserRequest;
import br.edu.espm.msav02.api.ApiModels.LoginRequest;
import br.edu.espm.msav02.api.ApiModels.LoginResponse;
import br.edu.espm.msav02.service.LoginService;
import br.edu.espm.msav02.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;

@RestController
public class UserController {
    private final LoginService loginService;
    private final UserService userService;

    public UserController(LoginService loginService, UserService userService) {
        this.loginService = loginService;
        this.userService = userService;
    }

    @Operation(summary = "Autentica um usuário")
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return new LoginResponse(loginService.login(request));
    }

    @Operation(summary = "Cadastra um usuário com role ROLE_USER")
    @PostMapping("/")
    public void create(@Valid @RequestBody CreateUserRequest request) {
        userService.create(request);
    }

    @Operation(summary = "Lista usuários e seus estados, sem expor senhas")
    @GetMapping("/")
    public java.util.List<ApiModels.UserResponse> list() {
        return userService.list();
    }

    @Operation(summary = "Troca a senha após validar a senha atual")
    @PutMapping("/trocarsenha/{userid}")
    public void changePassword(@PathVariable String userid, @Valid @RequestBody ChangePasswordRequest request) {
        if (userid.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Identificador inválido");
        }
        userService.changePassword(userid, request.atual(), request.nova());
    }

    @Operation(summary = "Desbloqueia um usuário")
    @PutMapping("/unlock/{userid}")
    public void unlock(@PathVariable String userid) {
        userService.unlock(userid);
    }
}
