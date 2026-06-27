package com.habittracker.backend.service;

import com.habittracker.backend.model.*;
import com.habittracker.backend.repository.*;
import com.habittracker.backend.dto.StepUpdatePayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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
    @Autowired private SimpMessagingTemplate messagingTemplate;

    // 🌟 Injected room task repositories
    @Autowired private RoomTaskRepository roomTaskRepository;
    @Autowired private RoomTaskLogRepository roomTaskLogRepository;

    // 1. Create a room (Creator becomes an APPROVED ADMIN instantly)
    @Transactional
    public Room createRoom(String name, Long creatorId, LocalDate targetMonth) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Creator not found"));

        Room room = new Room();
        room.setName(name);
        room.setTargetMonthYear(targetMonth.withDayOfMonth(1));
        room.setCreator(creator);
        room.setInviteCode(UUID.randomUUID().toString().substring(0, 6).toUpperCase());

        Room savedRoom = roomRepository.save(room);

        // Auto-join the creator as an APPROVED ADMIN
        RoomMemberId memberId = new RoomMemberId(savedRoom.getId(), creatorId);
        RoomMember adminMember = new RoomMember();
        adminMember.setId(memberId);
        adminMember.setRoom(savedRoom);
        adminMember.setUser(creator);
        adminMember.setCurrentStep(0);
        adminMember.setStatus(RoomMember.MembershipStatus.APPROVED);
        adminMember.setRole(RoomMember.RoomRole.ADMIN);

        roomMemberRepository.save(adminMember);
        return savedRoom;
    }

    // 2. Submit a request to Join a room via Invite Code (Saves as PENDING MEMBER)
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
        member.setStatus(RoomMember.MembershipStatus.PENDING);
        member.setRole(RoomMember.RoomRole.MEMBER);

        return roomMemberRepository.save(member);
    }

    // 3. Admin workflow actions: Approve or Reject a user's join request
    @Transactional
    public void processJoinRequest(Long roomId, Long targetUserId, Long adminId, boolean approve) {
        RoomMember adminCheck = roomMemberRepository.findByRoomIdAndUserId(roomId, adminId)
                .orElseThrow(() -> new RuntimeException("Admin context mapping not found"));

        if (adminCheck.getRole() != RoomMember.RoomRole.ADMIN) {
            throw new RuntimeException("Access Denied: Only the Room Admin can process join requests.");
        }

        RoomMemberId targetId = new RoomMemberId(roomId, targetUserId);
        RoomMember pendingMember = roomMemberRepository.findById(targetId)
                .orElseThrow(() -> new RuntimeException("Pending registration record not found"));

        if (approve) {
            pendingMember.setStatus(RoomMember.MembershipStatus.APPROVED);
            roomMemberRepository.save(pendingMember);
        } else {
            roomMemberRepository.delete(pendingMember);
        }
    }

    // 4. THE ORIGINAL PERSONAL LUDO ENGINE
    @Transactional
    public void evaluateLudoProgression(Long userId, LocalDate date, boolean isUndo) {
        long totalHabits = logRepository.countTotalHabitsForUser(userId);
        long completedHabits = logRepository.countCompletedHabitsForUserAndDate(userId, date);

        List<RoomMember> memberships = roomMemberRepository.findByUserId(userId);
        int dayOfMonth = date.getDayOfMonth();

        for (RoomMember member : memberships) {
            if (member.getStatus() != RoomMember.MembershipStatus.APPROVED) continue;

            if (isUndo) {
                if (completedHabits == totalHabits - 1 && member.getCurrentStep() > 0) {
                    member.setCurrentStep(member.getCurrentStep() - 1);
                    roomMemberRepository.save(member);
                    messagingTemplate.convertAndSend("/topic/room/" + member.getRoom().getId(),
                            new StepUpdatePayload(member.getRoom().getId(), userId, member.getCurrentStep()));
                }
            } else {
                if (totalHabits > 0 && totalHabits == completedHabits) {
                    if (member.getCurrentStep() < dayOfMonth) {
                        member.setCurrentStep(member.getCurrentStep() + 1);
                        roomMemberRepository.save(member);
                        messagingTemplate.convertAndSend("/topic/room/" + member.getRoom().getId(),
                                new StepUpdatePayload(member.getRoom().getId(), userId, member.getCurrentStep()));
                    }
                }
            }
        }
    }

    // 🌟 5. NEW ROOM ACCOUNTABILITY TASK ENGINE: Moves member token when all room tasks are completed
    @Transactional
    public void evaluateRoomTaskLudoProgression(Long roomId, Long userId, LocalDate date, boolean isUndo) {
        // 1. Fetch total tasks configured for this room arena
        List<RoomTask> roomTasks = roomTaskRepository.findByRoomId(roomId);
        long totalTasksCount = roomTasks.size();
        if (totalTasksCount == 0) return;

        // 2. Compute completed tasks for today
        List<Long> taskIds = roomTasks.stream().map(RoomTask::getId).toList();
        long completedTasksCount = roomTaskLogRepository
                .countByRoomTaskIdInAndUserIdAndLogDateAndCompletedTrue(taskIds, userId, date);

        RoomMemberId memberId = new RoomMemberId(roomId, userId);
        roomMemberRepository.findById(memberId).ifPresent(member -> {
            int dayOfMonth = date.getDayOfMonth();

            if (isUndo) {
                // ↩️ UNDO LOGIC: If they broke perfection (e.g., they had totalTasks, but now have totalTasks - 1)
                // AND their token had already advanced for today, pull it back by exactly 1.
                if (completedTasksCount == totalTasksCount - 1 && member.getCurrentStep() > 0) {
                    member.setCurrentStep(member.getCurrentStep() - 1);
                    roomMemberRepository.save(member);

                    System.out.println("↩️ Room Undo! Pulled User " + userId + " back to step " + member.getCurrentStep());

                    // Broadcast live state regression across WebSockets
                    messagingTemplate.convertAndSend("/topic/room/" + roomId,
                            new StepUpdatePayload(roomId, userId, member.getCurrentStep()));
                }
            } else {
                // 🚀 PROGRESS LOGIC: Only advance if ALL tasks are completed and they haven't stepped yet today
                if (totalTasksCount == completedTasksCount) {
                    if (member.getCurrentStep() < dayOfMonth) {
                        member.setCurrentStep(member.getCurrentStep() + 1);
                        roomMemberRepository.save(member);

                        System.out.println("🎉 Room Step Taken! User " + userId + " advanced to step " + member.getCurrentStep());

                        // Broadcast live state progression across WebSockets
                        messagingTemplate.convertAndSend("/topic/room/" + roomId,
                                new StepUpdatePayload(roomId, userId, member.getCurrentStep()));
                    }
                }
            }
        });
    }
}