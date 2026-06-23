package com.habittracker.backend.controller;

import com.habittracker.backend.model.User;
import com.habittracker.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    // 1. USER REGISTRATION
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String email = payload.get("email");
        String password = payload.get("password");

        // Validations
        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Username is already taken!"));
        }

        // Create user and hash their password securely
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password)); // Encrypts plain text password

        User savedUser = userRepository.save(user);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    // 2. USER LOGIN
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");

        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid username or password"));
        }

        User user = userOpt.get();

        // Compare plain text login password against the stored BCrypt hash
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Invalid username or password"));
        }

        // Return user info on successful login (Session or token management can be added on top)
        return ResponseEntity.ok(user);
    }
}