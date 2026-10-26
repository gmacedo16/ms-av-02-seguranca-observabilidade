package br.edu.espm.msav02.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Component;

@Component
public class DemoUserBootstrap implements ApplicationRunner {
    private final JdbcUserDetailsManager users;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public DemoUserBootstrap(JdbcUserDetailsManager users, PasswordEncoder passwordEncoder, Environment environment) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        seed("admin", "APP_BOOTSTRAP_ADMIN_PASSWORD", "ADMIN");
        seed("manutencao", "APP_BOOTSTRAP_MAINTENANCE_PASSWORD", "MANUTENCAO");
    }

    private void seed(String username, String passwordVariable, String role) {
        String password = environment.getProperty(passwordVariable);
        if (password == null || password.isBlank() || users.userExists(username)) {
            return;
        }
        users.createUser(User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles(role)
                .build());
    }
}
