package br.edu.espm.msav02.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    JdbcUserDetailsManager userDetailsManager(DataSource dataSource) {
        JdbcUserDetailsManager manager = new JdbcUserDetailsManager(dataSource);
        manager.setUsersByUsernameQuery("select username, senha, enabled from usuarios where username = ?");
        manager.setAuthoritiesByUsernameQuery("select username, authority from authorities where username = ?");
        manager.setUserExistsSql("select username from usuarios where username = ?");
        manager.setCreateUserSql("insert into usuarios (username, senha, enabled) values (?, ?, ?)");
        manager.setUpdateUserSql("update usuarios set senha = ?, enabled = ? where username = ?");
        manager.setDeleteUserSql("delete from usuarios where username = ?");
        manager.setDeleteUserAuthoritiesSql("delete from authorities where username = ?");
        manager.setCreateAuthoritySql("insert into authorities (username, authority) values (?, ?)");
        manager.setChangePasswordSql("update usuarios set senha = ? where username = ?");
        manager.setEnableGroups(false);
        manager.setRolePrefix("");
        return manager;
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/login", "/trocarsenha/**", "/v3/api-docs/**", "/swagger-ui/**",
                                "/swagger-ui.html", "/actuator/health", "/actuator/prometheus").permitAll()
                        .requestMatchers(HttpMethod.POST, "/").hasRole("MANUTENCAO")
                        .requestMatchers(HttpMethod.GET, "/").hasRole("MANUTENCAO")
                        .requestMatchers(HttpMethod.PUT, "/unlock/**").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
