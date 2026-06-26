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
        adminMember.setStatus(RoomMember.MembershipStatus.APPROVED); // 🌟 Admin is pre-approved
        adminMember.setRole(RoomMember.RoomRole.ADMIN);             // 🌟 Admin role assignment

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
        member.setStatus(RoomMember.MembershipStatus.PENDING); // 🌟 Requires Admin authorization
        member.setRole(RoomMember.RoomRole.MEMBER);           // 🌟 Base role

        return roomMemberRepository.save(member);
    }

    // 3. Admin workflow actions: Approve or Reject a user's join request
    @Transactional
    public void processJoinRequest(Long roomId, Long targetUserId, Long adminId, boolean approve) {
        // Enforce security guard check
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
            roomMemberRepository.delete(pendingMember); // Clear from database if rejected
        }
    }

    // 4. THE LUDO ENGINE (Unchanged, evaluating step progression for APPROVED users only)
    @Transactional
    public void evaluateLudoProgression(Long userId, LocalDate date, boolean isUndo) {
        long totalHabits = logRepository.countTotalHabitsForUser(userId);
        long completedHabits = logRepository.countCompletedHabitsForUserAndDate(userId, date);

        List<RoomMember> memberships = roomMemberRepository.findByUserId(userId);
        int dayOfMonth = date.getDayOfMonth();

        for (RoomMember member : memberships) {
            // 🌟 Safety Guard: Only advance users whose access requests are APPROVED
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
}