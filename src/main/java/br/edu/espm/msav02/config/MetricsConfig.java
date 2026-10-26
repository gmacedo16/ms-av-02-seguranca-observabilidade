package br.edu.espm.msav02.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import br.edu.espm.msav02.service.LoginAttemptService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Configuration
public class MetricsConfig {
    @Bean
    Gauge lockedUsersGauge(MeterRegistry registry, LoginAttemptService attempts) {
        return Gauge.builder("app.locked.users", attempts, LoginAttemptService::countLockedUsers)
                .description("Número atual de usuários bloqueados")
                .register(registry);
    }
}
