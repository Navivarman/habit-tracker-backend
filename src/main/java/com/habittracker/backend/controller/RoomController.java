package com.habittracker.backend.controller;

import com.habittracker.backend.model.Room;
import com.habittracker.backend.model.RoomMember;
import com.habittracker.backend.repository.RoomMemberRepository;
import com.habittracker.backend.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "http://localhost:5173")
public class RoomController {

    @Autowired private RoomService roomService;
    @Autowired private RoomMemberRepository roomMemberRepository;

    // 1. Create a challenge room
    @PostMapping
    public ResponseEntity<Room> createRoom(@RequestBody Map<String, Object> payload) {
        String name = payload.get("name").toString();
        Long creatorId = Long.valueOf(payload.get("creatorId").toString());
        LocalDate targetMonth = LocalDate.parse(payload.get("targetMonth").toString());

        Room room = roomService.createRoom(name, creatorId, targetMonth);
        return ResponseEntity.ok(room);
    }

    // 2. Join a room via Invite Code
    @PostMapping("/join")
    public ResponseEntity<RoomMember> joinRoom(@RequestBody Map<String, String> payload) {
        String inviteCode = payload.get("inviteCode");
        Long userId = Long.valueOf(payload.get("userId"));

        RoomMember member = roomService.joinRoom(inviteCode, userId);
        return ResponseEntity.ok(member);
    }

    // 3. Get leaderboard/standings for a room
    @GetMapping("/{roomId}/leaderboard")
    public ResponseEntity<List<RoomMember>> getLeaderboard(@PathVariable Long roomId) {
        List<RoomMember> standings = roomMemberRepository.findByRoomIdOrderByCurrentStepDesc(roomId);
        return ResponseEntity.ok(standings);
    }
}