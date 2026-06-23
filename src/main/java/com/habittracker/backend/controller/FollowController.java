package com.habittracker.backend.controller;

import com.habittracker.backend.model.Follow;
import com.habittracker.backend.model.User;
import com.habittracker.backend.repository.FollowRepository;
import com.habittracker.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/social")
@CrossOrigin(origins = "*")
public class FollowController {

    @Autowired private FollowRepository followRepository;
    @Autowired private UserRepository userRepository;

    @PostMapping("/follow")
    @Transactional
    public ResponseEntity<String> followUser(@RequestBody Map<String, Long> payload) {
        Long followerId = payload.get("followerId");
        Long followedId = payload.get("followedId");

        if (followerId.equals(followedId)) return ResponseEntity.badRequest().body("You cannot follow yourself.");

        User follower = userRepository.findById(followerId).orElseThrow();
        User followed = userRepository.findById(followedId).orElseThrow();

        if (followRepository.findByFollowerIdAndFollowedId(followerId, followedId).isPresent()) {
            return ResponseEntity.badRequest().body("Already following this user.");
        }

        Follow follow = new Follow();
        follow.setFollower(follower);
        follow.setFollowed(followed);
        followRepository.save(follow);

        return ResponseEntity.ok("Successfully followed " + followed.getUsername());
    }

    @PostMapping("/unfollow")
    @Transactional
    public ResponseEntity<String> unfollowUser(@RequestBody Map<String, Long> payload) {
        Long followerId = payload.get("followerId");
        Long followedId = payload.get("followedId");

        Follow follow = followRepository.findByFollowerIdAndFollowedId(followerId, followedId)
                .orElseThrow(() -> new RuntimeException("Follow relationship not found."));

        followRepository.delete(follow);
        return ResponseEntity.ok("Successfully unfollowed user.");
    }

    @GetMapping("/user/{userId}/following")
    public ResponseEntity<List<User>> getFollowing(@PathVariable Long userId) {
        return ResponseEntity.ok(followRepository.findFollowingByUserId(userId));
    }
}