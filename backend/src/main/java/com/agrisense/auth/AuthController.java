package com.agrisense.auth;

import com.agrisense.security.JwtService;
import com.agrisense.user.User;
import com.agrisense.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
    }

    public record Credentials(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 8, max = 100) String password) {}

    public record TokenResponse(String token, String email) {}

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(@Valid @RequestBody Credentials req) {
        if (users.existsByEmail(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email đã tồn tại");
        }
        User user = users.save(new User(req.email(), encoder.encode(req.password())));
        String token = jwt.generateToken(user.getId(), user.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new TokenResponse(token, user.getEmail()));
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody Credentials req) {
        User user = users.findByEmail(req.email())
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu sai"));
        return new TokenResponse(jwt.generateToken(user.getId(), user.getEmail()), user.getEmail());
    }
}
