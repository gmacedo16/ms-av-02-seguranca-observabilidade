package br.edu.espm.msav02.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginAttemptService {
    private final JdbcTemplate jdbc;
    private final JdbcUserDetailsManager users;
    private final int maxFailedAttempts;

    public LoginAttemptService(JdbcTemplate jdbc, JdbcUserDetailsManager users,
                               @Value("${app.login.max-failed-attempts:2}") int maxFailedAttempts) {
        this.jdbc = jdbc;
        this.users = users;
        this.maxFailedAttempts = maxFailedAttempts;
    }

    public boolean isLocked(String username) {
        Integer count = jdbc.queryForObject(
                "select count(*) from login_attempts where username = ? and locked = true",
                Integer.class, username);
        return count != null && count > 0;
    }

    @Transactional
    public void recordFailure(String username) {
        int updated = jdbc.update("""
                update login_attempts
                   set failed_attempts = failed_attempts + 1,
                       last_failed_at = CURRENT_TIMESTAMP,
                       updated_at = CURRENT_TIMESTAMP
                 where username = ? and locked = false
                """, username);
        if (updated == 0) {
            jdbc.update("""
                    insert into login_attempts
                        (username, failed_attempts, locked, last_failed_at, updated_at)
                    values (?, 1, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                    """, username);
        }

        Integer attempts = jdbc.queryForObject(
                "select failed_attempts from login_attempts where username = ?",
                Integer.class, username);
        if (attempts != null && attempts >= maxFailedAttempts && users.userExists(username)) {
            var user = users.loadUserByUsername(username);
            users.updateUser(User.withUserDetails(user).disabled(true).build());
            jdbc.update("""
                    update login_attempts
                       set locked = true, locked_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                     where username = ?
                    """, username);
        }
    }

    @Transactional
    public void reset(String username) {
        jdbc.update("""
                merge into login_attempts (username, failed_attempts, locked, last_failed_at, locked_at, updated_at)
                key (username) values (?, 0, false, null, null, CURRENT_TIMESTAMP)
                """, username);
    }

    public long countLockedUsers() {
        Long count = jdbc.queryForObject("""
                select count(*)
                  from login_attempts a
                  join usuarios u on u.username = a.username
                 where a.locked = true
                """, Long.class);
        return count == null ? 0 : count;
    }
}
