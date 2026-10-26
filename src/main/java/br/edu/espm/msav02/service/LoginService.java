package br.edu.espm.msav02.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import br.edu.espm.msav02.api.ApiModels.LoginRequest;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

@Service
public class LoginService {
    private final AuthenticationManager authenticationManager;
    private final LoginAttemptService attempts;
    private final Counter successfulLogins;
    private final Counter failedLogins;

    public LoginService(AuthenticationManager authenticationManager, LoginAttemptService attempts,
                        MeterRegistry meterRegistry) {
        this.authenticationManager = authenticationManager;
        this.attempts = attempts;
        this.successfulLogins = meterRegistry.counter("app.login.success");
        this.failedLogins = meterRegistry.counter("app.login.failure");
    }

    public String login(LoginRequest request) {
        if (attempts.isLocked(request.username())) {
            failedLogins.increment();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário ou senha inválidos");
        }
        try {
            var authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.senha()));
            attempts.reset(request.username());
            successfulLogins.increment();
            return authentication.getName();
        } catch (AuthenticationException exception) {
            attempts.recordFailure(request.username());
            failedLogins.increment();
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário ou senha inválidos");
        }
    }
}
