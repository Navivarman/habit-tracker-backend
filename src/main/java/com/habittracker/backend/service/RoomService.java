package com.habittracker.backend.service;

import com.habittracker.backend.model.*;
import com.habittracker.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class RoomService {

    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomMemberRepository roomMemberRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private DailyHabitLogRepository logRepository;

    // 1. Create a room with a random 6-character alphanumeric invite code
    @Transactional
    public Room createRoom(String name, Long creatorId, LocalDate targetMonth) {
        User creator = userRepository.findById(creatorId).orElseThrow();

        Room room = new Room();
        room.setName(name);
        room.setTargetMonthYear(targetMonth.withDayOfMonth(1)); // Lock to 1st of month
        room.setCreator(creator);
        room.setInviteCode(UUID.randomUUID().toString().substring(0, 6).toUpperCase());

        Room savedRoom = roomRepository.save(room);

        // Auto-join the creator to their own room
        joinRoom(savedRoom.getInviteCode(), creatorId);

        return savedRoom;
    }

    // 2. Join a room via Invite Code
    @Transactional
    public RoomMember joinRoom(String inviteCode, Long userId) {
        Room room = roomRepository.findByInviteCode(inviteCode.toUpperCase())
                .orElseThrow(() -> new RuntimeException("Invalid invite code"));
        User user = userRepository.findById(userId).orElseThrow();

        RoomMemberId memberId = new RoomMemberId(room.getId(), user.getId());
        if (roomMemberRepository.existsById(memberId)) {
            return roomMemberRepository.findById(memberId).get();
        }

        RoomMember member = new RoomMember();
        member.setId(memberId);
        member.setRoom(room);
        member.setUser(user);
        member.setCurrentStep(0);

        return roomMemberRepository.save(member);
    }

    // 3. 🏁 THE LUDO ENGINE: Evaluate and move token if all habits are completed
    @Transactional
    public void evaluateLudoProgression(Long userId, LocalDate date) {
        long totalHabits = logRepository.countTotalHabitsForUser(userId);
        long completedHabits = logRepository.countCompletedHabitsForUserAndDate(userId, date);

        // Safeguard: User must have at least 1 habit assigned to progress
        if (totalHabits > 0 && totalHabits == completedHabits) {
            List<RoomMember> memberships = roomMemberRepository.findByUserId(userId);

            for (RoomMember member : memberships) {
                // Advance 1 step
                member.setCurrentStep(member.getCurrentStep() + 1);
                roomMemberRepository.save(member);
                System.out.println("🎉 User " + userId + " advanced to step " + member.getCurrentStep() + " in room " + member.getRoom().getName());
            }
        }
    }
}