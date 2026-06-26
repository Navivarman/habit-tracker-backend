package com.habittracker.backend.controller;

import com.habittracker.backend.model.*;
import com.habittracker.backend.repository.*;
import com.habittracker.backend.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional; // 🌟 Added for explicit transactional operations
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*", allowedHeaders = "*")
public class RoomController {

    @Autowired private RoomService roomService;
    @Autowired private RoomMemberRepository roomMemberRepository;
    @Autowired private RoomRepository roomRepository; // 🌟 Added dependency
    @Autowired private UserRepository userRepository; // 🌟 Added dependency

    // 1. Create a challenge room
    @PostMapping
    public ResponseEntity<Room> createRoom(@RequestBody Map<String, Object> payload) {
        String name = payload.get("name").toString();
        Long creatorId = Long.valueOf(payload.get("creatorId").toString());
        LocalDate targetMonth = LocalDate.parse(payload.get("targetMonth").toString());

        Room room = roomService.createRoom(name, creatorId, targetMonth);
        return ResponseEntity.ok(room);
    }

    // 2. Submit a request to join a room via Invite Code (Returns PENDING status)
    @PostMapping("/join")
    public ResponseEntity<RoomMember> joinRoom(@RequestBody Map<String, String> payload) {
        String inviteCode = payload.get("inviteCode");
        Long userId = Long.valueOf(payload.get("userId"));

        RoomMember member = roomService.joinRoom(inviteCode, userId);
        return ResponseEntity.ok(member);
    }

    // 3. Fetch all rooms the current user has already joined (APPROVED rooms only)
    @GetMapping("/joined/{userId}")
    public ResponseEntity<List<RoomMember>> getJoinedRooms(@PathVariable Long userId) {
        List<RoomMember> rooms = roomMemberRepository.findApprovedRoomsByUserId(userId);
        return ResponseEntity.ok(rooms);
    }

    // 4. Fetch pending join requests for a room (Admin Dashboard view utility)
    @GetMapping("/{roomId}/requests")
    public ResponseEntity<List<RoomMember>> getPendingRequests(@PathVariable Long roomId) {
        List<RoomMember> pendingRequests = roomMemberRepository.findPendingRequestsByRoomId(roomId);
        return ResponseEntity.ok(pendingRequests);
    }

    // 5. Admin Action Endpoint: Accept or Reject a pending user request
    @PutMapping("/{roomId}/requests/{targetUserId}")
    public ResponseEntity<?> updateRequestStatus(
            @PathVariable Long roomId,
            @PathVariable Long targetUserId,
            @RequestParam Long adminId,
            @RequestParam boolean approve) {
        try {
            roomService.processJoinRequest(roomId, targetUserId, adminId, approve);
            return ResponseEntity.ok(Map.of("message", "Request evaluation processed cleanly."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 6. Get leaderboard standings for a room (Only for APPROVED room members)
    @GetMapping("/{roomId}/leaderboard")
    public ResponseEntity<List<RoomMember>> getLeaderboard(@PathVariable Long roomId) {
        List<RoomMember> standings = roomMemberRepository.findByRoomIdOrderByCurrentStepDesc(roomId);
        return ResponseEntity.ok(standings);
    }

    // 7. DISCOVER: Get all rooms created globally (alongside their creator names)
    @GetMapping("/discover")
    public ResponseEntity<List<Room>> getAllGlobalRooms() {
        return ResponseEntity.ok(roomRepository.findAll());
    }

    // 8. DISCOVER: Get all request rows for a user to calculate button states on the frontend
    @GetMapping("/user-memberships/{userId}")
    public ResponseEntity<List<RoomMember>> getUserMemberships(@PathVariable Long userId) {
        return ResponseEntity.ok(roomMemberRepository.findByUserId(userId));
    }

    // 9. LEAVE: Leave an approved room or cancel a pending request
    @DeleteMapping("/{roomId}/leave")
    @Transactional // 🌟 Enforces safe hibernate execution context block
    public ResponseEntity<?> leaveRoom(@PathVariable Long roomId, @RequestParam Long userId) {
        roomMemberRepository.deleteByRoomIdAndUserId(roomId, userId);
        return ResponseEntity.ok(Map.of("message", "Successfully left the room environment."));
    }

    // 10. NO-CODE JOIN: Submit a join request directly by clicking a specific Room ID
    @PostMapping("/{roomId}/join-direct")
    public ResponseEntity<RoomMember> joinRoomDirect(@PathVariable Long roomId, @RequestParam Long userId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RoomMemberId memberId = new RoomMemberId(roomId, userId);
        if (roomMemberRepository.existsById(memberId)) {
            return ResponseEntity.ok(roomMemberRepository.findById(memberId).get());
        }

        RoomMember member = new RoomMember();
        member.setId(memberId);
        member.setRoom(room);
        member.setUser(user);
        member.setCurrentStep(0);
        member.setStatus(RoomMember.MembershipStatus.PENDING); // Enters Pending State
        member.setRole(RoomMember.RoomRole.MEMBER);

        return ResponseEntity.ok(roomMemberRepository.save(member));
    }
}