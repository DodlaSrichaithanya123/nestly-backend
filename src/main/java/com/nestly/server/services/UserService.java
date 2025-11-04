package com.nestly.server.services;

import com.nestly.server.models.Role;
import com.nestly.server.models.User;
import com.nestly.server.repositories.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    // ✅ Constructor Injection
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    // Add this method
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    // ✅ Register a new user
    public User registerUser(User user) {
        // 🔹 Check if username already exists
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        // 🔹 Check if email already exists
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        // 🔹 Encode password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 🔹 Set default role (if not provided)
        user.setRole(user.getRole() != null ? user.getRole() : Role.USER);

        // 🔹 Enable the account by default
        user.setEnabled(true);

        // 🔹 Save to database
        return userRepository.save(user);
    }

    // ✅ Login (authenticate) user using email
    public User loginUserByEmail(String email, String password) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            throw new RuntimeException("Invalid email or password");
        }

        User user = optionalUser.get();

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid email or password");
        }

        return user;
    }

    // ✅ Fetch a user by ID
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    // ✅ Fetch a user by username
    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    // ✅ Delete a user (for admin use later)
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        userRepository.deleteById(id);
    }

    // ✅ Forgot Password logic
    public void updatePassword(String email, String newPassword) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (optionalUser.isEmpty()) {
            throw new RuntimeException("No account found with this email");
        }

        User user = optionalUser.get();
        user.setPassword(passwordEncoder.encode(newPassword)); // hash new password
        userRepository.save(user);
    }
}
