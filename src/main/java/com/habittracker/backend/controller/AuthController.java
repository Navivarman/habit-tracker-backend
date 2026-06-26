package com.habittracker.backend.controller;

import com.habittracker.backend.config.JwtUtil;
import com.habittracker.backend.model.User;
import com.habittracker.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private UserRepository userRepository;
    @Autowired private JwtUtil jwtUtil;

    // 1️⃣ PUBLIC REGISTRATION ENDPOINT
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Error: Username is already taken!");
        }

        // For development, we store a basic string placeholder.
        // Note: Production builds should pass this through a BCryptPasswordEncoder!
        user.setPasswordHash(user.getPasswordHash());
        User savedUser = userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
    }

    // 2️⃣ PUBLIC LOGIN ENDPOINT (Issues Cryptographic Session JWT Tokens)
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        Optional<User> userOpt = userRepository.findByUsername(username);

        // Simple validation check against our user entity credentials
        if (userOpt.isPresent() && userOpt.get().getPasswordHash().equals(password)) {
            User user = userOpt.get();
            String generatedToken = jwtUtil.generateToken(username);

            // Construct a comprehensive map payload returning session attributes back to React
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("token", generatedToken);
            responseData.put("id", user.getId());
            responseData.put("username", user.getUsername());
            responseData.put("email", user.getEmail());

            return ResponseEntity.ok(responseData);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error: Invalid username or password credentials!");
    }
}