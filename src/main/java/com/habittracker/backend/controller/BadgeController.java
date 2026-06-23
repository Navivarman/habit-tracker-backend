package com.habittracker.backend.controller;

import com.habittracker.backend.model.Badge;
import com.habittracker.backend.repository.BadgeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/badges")
@CrossOrigin(origins = "*")
public class BadgeController {

    @Autowired private BadgeRepository badgeRepository;

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Badge>> getUserBadges(@PathVariable Long userId) {
        return ResponseEntity.ok(badgeRepository.findByUserId(userId));
    }
}