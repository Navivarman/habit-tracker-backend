package com.habittracker.backend.service;

import com.habittracker.backend.model.*;
import com.habittracker.backend.repository.*;
import com.habittracker.backend.dto.StepUpdatePayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate; // 👈 Required for WebSockets
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
    @Autowired private SimpMessagingTemplate messagingTemplate; // 👈 Injected message broker dispatcher

    // 1. Create a room with a random 6-character alphanumeric invite code
    @Transactional
    public Room createRoom(String name, Long creatorId, LocalDate targetMonth) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Creator not found"));

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
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

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

    // 3. 🏁 THE LUDO ENGINE: Evaluate and move token if all habits are completed + Broadcast over WebSockets
    @Transactional
    public void evaluateLudoProgression(Long userId, LocalDate date, boolean isUndo) {
        long totalHabits = logRepository.countTotalHabitsForUser(userId);
        long completedHabits = logRepository.countCompletedHabitsForUserAndDate(userId, date);

        List<RoomMember> memberships = roomMemberRepository.findByUserId(userId);
        int dayOfMonth = date.getDayOfMonth();

        for (RoomMember member : memberships) {
            if (isUndo) {
                // ↩️ UNDO LOGIC: If it was a perfect day but the user just unchecked a habit
                // and their token had already advanced for today, pull it back by 1.
                if (completedHabits == totalHabits - 1 && member.getCurrentStep() > 0) {
                    member.setCurrentStep(member.getCurrentStep() - 1);
                    roomMemberRepository.save(member);
                    System.out.println("↩️ Undo detected! Pulled User " + userId + " back to step " + member.getCurrentStep());

                    // 🚀 Live Stream the regression change to everyone in the room
                    messagingTemplate.convertAndSend("/topic/room/" + member.getRoom().getId(),
                            new StepUpdatePayload(member.getRoom().getId(), userId, member.getCurrentStep()));
                }
            } else {
                // 🚀 PROGRESS LOGIC: If all required habits are now completed
                if (totalHabits > 0 && totalHabits == completedHabits) {
                    // Only advance if they haven't already taken their step for today
                    if (member.getCurrentStep() < dayOfMonth) {
                        member.setCurrentStep(member.getCurrentStep() + 1);
                        roomMemberRepository.save(member);
                        System.out.println("🎉 Legit step! User " + userId + " moved forward to step " + member.getCurrentStep());

                        // 🚀 Live Stream the advancement change to everyone in the room
                        messagingTemplate.convertAndSend("/topic/room/" + member.getRoom().getId(),
                                new StepUpdatePayload(member.getRoom().getId(), userId, member.getCurrentStep()));
                    }
                }
            }
        }
    }
}