package br.edu.espm.msav02.service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.edu.espm.msav02.api.ApiModels.CreateUserRequest;
import br.edu.espm.msav02.api.ApiModels.UserResponse;
import br.edu.espm.msav02.service.LoginAttemptService;

@Service
public class UserService {
    private final JdbcTemplate jdbc;
    private final JdbcUserDetailsManager users;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService attempts;

    public UserService(JdbcTemplate jdbc, JdbcUserDetailsManager users, PasswordEncoder passwordEncoder,
                       LoginAttemptService attempts) {
        this.jdbc = jdbc;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.attempts = attempts;
    }

    @Transactional
    public void create(CreateUserRequest request) {
        if (users.userExists(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Usuário já cadastrado");
        }
        users.createUser(User.withUsername(request.username())
                .password(passwordEncoder.encode(request.senha()))
                .roles("USER")
                .build());
    }

    public List<UserResponse> list() {
        Map<String, UserAccumulator> results = new LinkedHashMap<>();
        return jdbc.query("""
                select u.username, u.enabled, a.authority, coalesce(la.locked, false) as locked
                  from usuarios u
                  left join authorities a on a.username = u.username
                  left join login_attempts la on la.username = u.username
                 order by u.username, a.authority
                """, row -> {
            while (row.next()) {
                String username = row.getString("username");
                UserAccumulator item = results.get(username);
                if (item == null) {
                    item = new UserAccumulator(row.getBoolean("enabled"), row.getBoolean("locked"));
                    results.put(username, item);
                }
                String authority = row.getString("authority");
                if (authority != null) {
                    item.roles.add(authority);
                }
            }
            return results.entrySet().stream()
                    .map(entry -> new UserResponse(entry.getKey(), Set.copyOf(entry.getValue().roles),
                            entry.getValue().enabled, entry.getValue().locked))
                    .toList();
        });
    }

    @Transactional
    public void changePassword(String username, String currentPassword, String newPassword) {
        if (!users.userExists(username)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário ou senha inválidos");
        }
        var user = users.loadUserByUsername(username);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário ou senha inválidos");
        }
        jdbc.update("update usuarios set senha = ? where username = ?",
                passwordEncoder.encode(newPassword), username);
    }

    @Transactional
    public void unlock(String username) {
        if (!users.userExists(username)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado");
        }
        var user = users.loadUserByUsername(username);
        users.updateUser(User.withUserDetails(user).disabled(false).build());
        attempts.reset(username);
    }

    private static final class UserAccumulator {
        private final boolean enabled;
        private final boolean locked;
        private final Set<String> roles = new LinkedHashSet<>();

        private UserAccumulator(boolean enabled, boolean locked) {
            this.enabled = enabled;
            this.locked = locked;
        }

    }
}
