package com.nestly.server.controllers;

import com.nestly.server.models.LoginRequest;
import com.nestly.server.models.User;
import com.nestly.server.config.JwtUtil;
import com.nestly.server.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "${frontend.url:http://localhost:5173}")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    // ✅ Constructor Injection
    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    // ✅ Register new user
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        try {
            User savedUser = userService.registerUser(user);
            return ResponseEntity.ok(savedUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestParam String email) {
        try {
            User user = userService.findByEmail(email);
            if (user == null) {
                return ResponseEntity.badRequest().body("User not found");
            }
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Server error: " + e.getMessage());
        }
    }

    // ✅ Login existing user
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            User user = userService.loginUserByEmail(request.getEmail(), request.getPassword());
            System.out.println("✅ User fetched from DB: " + user);
            String token = jwtUtil.generateToken(user.getEmail());

            // ✅ Include userId, email, role, and token in response
            return ResponseEntity.ok(new LoginResponse(
                    token,
                    user.getId(),
                    user.getEmail(),
                    user.getRole().name()));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String newPassword = request.get("newPassword");
        try {
            userService.updatePassword(email, newPassword);
            return ResponseEntity.ok("Password updated successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ✅ Simple test endpoint
    @GetMapping("/test")
    public String test() {
        return "Auth API working!";
    }

    // ✅ Inner static class for response
    private static class LoginResponse {
        private String token;
        private Long id;
        private String email;
        private String role;

        public LoginResponse(String token, Long id, String email, String role) {
            this.token = token;
            this.id = id;
            this.email = email;
            this.role = role;
        }

        public String getToken() {
            return token;
        }

        public Long getId() {
            return id;
        }

        public String getEmail() {
            return email;
        }

        public String getRole() {
            return role;
        }
    }
}
