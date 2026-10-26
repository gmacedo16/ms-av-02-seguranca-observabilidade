package br.edu.espm.msav02;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MsAv02ApplicationTests {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private JdbcUserDetailsManager users;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        jdbc.update("delete from login_attempts");
        jdbc.update("delete from authorities");
        jdbc.update("delete from usuarios");
        createUser("admin", "admin-password", "ADMIN");
        createUser("manutencao", "maintenance-password", "MANUTENCAO");
        createUser("aluno", "student-password", "USER");
    }

    @Test
    void loginBloqueiaNaSegundaFalhaEAdminPodeDesbloquear() throws Exception {
        mvc.perform(put("/unlock/aluno")).andExpect(status().isUnauthorized());
        mvc.perform(put("/unlock/aluno").with(httpBasic("manutencao", "maintenance-password")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/login").contentType("application/json")
                        .content("{\"username\":\"aluno\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/login").contentType("application/json")
                        .content("{\"username\":\"aluno\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/login").contentType("application/json")
                        .content("{\"username\":\"aluno\",\"senha\":\"student-password\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/unlock/aluno").with(httpBasic("admin", "admin-password")))
                .andExpect(status().isOk());
        mvc.perform(post("/login").contentType("application/json")
                        .content("{\"username\":\"aluno\",\"senha\":\"student-password\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"username\":\"aluno\"")));
    }

    @Test
    void endpointsDeUsuariosExigemRoleDeManutencao() throws Exception {
        mvc.perform(get("/")).andExpect(status().isUnauthorized());
        mvc.perform(get("/").with(httpBasic("aluno", "student-password")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/").with(httpBasic("manutencao", "maintenance-password")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"username\":\"aluno\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("student-password"))));
        mvc.perform(post("/").with(httpBasic("manutencao", "maintenance-password"))
                        .contentType("application/json")
                        .content("{\"username\":\"novo\",\"senha\":\"new-user-password\"}"))
                .andExpect(status().isOk());
        org.junit.jupiter.api.Assertions.assertTrue(users.userExists("novo"));
        org.junit.jupiter.api.Assertions.assertTrue(passwordEncoder.matches("new-user-password",
                users.loadUserByUsername("novo").getPassword()));
    }

    @Test
    void trocaSenhaPublicaValidaSenhaAtual() throws Exception {
        mvc.perform(put("/trocarsenha/aluno").contentType("application/json")
                        .content("{\"atual\":\"errada\",\"nova\":\"new-student-password\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/trocarsenha/aluno").contentType("application/json")
                        .content("{\"atual\":\"student-password\",\"nova\":\"new-student-password\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/login").contentType("application/json")
                        .content("{\"username\":\"aluno\",\"senha\":\"new-student-password\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void documentacaoEEndpointPrometheusEstaoDisponiveis() throws Exception {
        mvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
        mvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("app_locked_users")));
    }

    private void createUser(String username, String password, String role) {
        users.createUser(User.withUsername(username)
                .password(passwordEncoder.encode(password))
                .roles(role)
                .build());
    }
}
