package com.gmu.erp;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(originPatterns = "http://localhost:*")
public class AuthController {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    public AuthController(JdbcTemplate jdbc, PasswordEncoder encoder) { this.jdbc = jdbc; this.encoder = encoder; }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        var rows = jdbc.queryForList("SELECT u.id,u.full_name,u.email,u.password_hash,r.name AS role FROM users u JOIN roles r ON r.id=u.role_id WHERE u.email=? AND u.active=true", request.email());
        if (rows.isEmpty()) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid email or password."));
        var user = rows.getFirst();
        if (!String.valueOf(user.get("role")).equals(request.role()) || !encoder.matches(request.password(), String.valueOf(user.get("password_hash")))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "This account does not match the selected portal, or the password is incorrect."));
        }
        return ResponseEntity.ok(Map.of("id", user.get("id"), "name", user.get("full_name"), "email", user.get("email"), "role", user.get("role")));
    }
    public record LoginRequest(String email, String password, String role) {}
}
