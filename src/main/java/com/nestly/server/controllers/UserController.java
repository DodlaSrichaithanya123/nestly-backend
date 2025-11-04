package com.nestly.server.controllers;

import com.nestly.server.models.User;
import com.nestly.server.services.UserService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "${frontend.url:http://localhost:5173}")
public class UserController {

    private final UserService userService;

    // ✅ Constructor injection
    public UserController(UserService userService) {
        this.userService = userService;
    }

    // ✅ Register endpoint
    @PostMapping("/register")
    public User registerUser(@RequestBody User user) {
        return userService.registerUser(user);
    }

    // ✅ Fetch user by ID
    @GetMapping("/{id}")
    public User getUserById(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    // ✅ Simple test endpoint
    @GetMapping("/test")
    public String test() {
        return "User API working!";
    }

    // ✅ Inner static class for login payload
    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
